package impl

import model.Member
import table.MemberTable
import port.CreateMemberAccountCommand
import port.IssueMemberTokenCommand
import port.MemberAccountRef
import port.MemberIdentityAdapter
import port.MemberIdentityProvider
import port.MemberPasswordChangedEvent
import port.MemberTokenBundle
import port.RefreshMemberTokenCommand
import neton.core.http.HttpException
import neton.core.http.NetonErrorCode
import neton.database.api.DbContext
import neton.database.dsl.*
import setting.MemberSettingKeys
import setting.currentValue
import neton.security.identity.UserId
import neton.security.jwt.JwtAuthenticator
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 内置身份后端（自托管账号，无外部 server）。
 *
 * - 账号：按 mobile/username 命中 member_users 复用 id，否则取数据库序列
 *   `member_users_id_seq` 的下一个值（普通自增主键，JS 安全的小整数）。
 * - token：复用应用的 [JwtAuthenticator]（HS256 + security.jwt.secretKey），与 admin token
 *   同一套签验，`/app` 路由组直接验通，`Identity.id = sub`。
 * - 会话：token 携带 `sid`(session_version)/`did`(device)；改密 bump session_version。
 *   v1 仅签发携带 + 刷新处校验；接口访问强校验（登出全端即时失效）留 v2 的 member_sessions guard。
 *
 * privchat 模式由 PrivchatMemberIdentityAdapter override（走 privchat-server）。
 */
class BuiltinMemberIdentityAdapter(
    private val jwt: JwtAuthenticator,
    private val db: DbContext,
) : MemberIdentityAdapter {

    companion object {
        const val ACCESS_TTL_SECONDS = 7200L          // 2h
        const val REFRESH_TTL_SECONDS = 2592000L      // 30d
        private val SCOPE = listOf("user")
        private const val ISSUER = "yese-builtin"
    }

    override suspend fun createOrBindAccount(command: CreateMemberAccountCommand): MemberAccountRef {
        val mobile = command.mobile?.takeIf { it.isNotBlank() }
        val username = command.username?.takeIf { it.isNotBlank() }
        val existing = when {
            mobile != null -> MemberTable.oneWhere { Member::mobile eq mobile }
            username != null -> MemberTable.oneWhere { Member::username eq username }
            else -> null
        }
        if (existing != null) {
            return MemberAccountRef(MemberIdentityProvider.BUILTIN, existing.id, created = false)
        }
        return MemberAccountRef(MemberIdentityProvider.BUILTIN, nextMemberId(), created = true)
    }

    /** 数据库序列取下一个主键（自增小整数，JS 安全）。 */
    private suspend fun nextMemberId(): Long =
        db.fetchAll("SELECT nextval('member_users_id_seq') AS id")
            .first().long("id")

    override suspend fun issueToken(command: IssueMemberTokenCommand): MemberTokenBundle {
        val deviceId = command.deviceId?.takeIf { it.isNotBlank() } ?: mintDeviceId()
        val member = MemberTable.get(command.memberId)
        var sessionVersion = member?.sessionVersion ?: 0L

        if (MemberSettingKeys.SINGLE_DEVICE_LOGIN.currentValue()) {
            val previous = member?.currentDeviceId
            if (previous != null && previous != deviceId) {
                // 换设备登录顶掉上一台：自增而不是读后写，并发登录才不会互相覆盖。
                // 自增后必须回读真实值 —— 本地 +1 在并发下会算出一个别人已经用掉的
                // 版本号，签出的 token 一刷新就失效。
                MemberTable.query { where { Member::id eq command.memberId } }
                    .update { increment(Member::sessionVersion) }
                sessionVersion = MemberTable.get(command.memberId)?.sessionVersion ?: (sessionVersion + 1)
            }
            if (member != null && previous != deviceId) {
                MemberTable.update(member.copy(sessionVersion = sessionVersion, currentDeviceId = deviceId))
            }
        }

        return buildBundle(command.memberId, deviceId, sessionVersion, deviceCreated = command.deviceId.isNullOrBlank())
    }

    /**
     * 刷新令牌。失效原因（签名坏 / 类型不对 / 主体缺失 / session_version 已 bump）在语义上
     * 都是 **401 需要重新登录**。此前抛裸 [IllegalArgumentException] → HTTP 500，客户端
     * 不会清理会话、不会重新登录，只会带着废 token 继续重试。
     *
     * 注意：只把**已识别**的失效原因映射成 401，其它异常照旧上抛为 500，
     * 不做「IllegalArgumentException 一律 400/401」的全局降级（会掩盖真实故障）。
     */
    override suspend fun refreshToken(command: RefreshMemberTokenCommand): MemberTokenBundle {
        val verified = try {
            jwt.verifyToken(command.refreshToken)
        } catch (e: Exception) {
            throw HttpException(NetonErrorCode.INVALID_TOKEN, "INVALID_REFRESH_TOKEN")
        }
        if (verified.claimString("type") != "refresh") {
            throw HttpException(NetonErrorCode.INVALID_TOKEN, "NOT_A_REFRESH_TOKEN")
        }
        val memberId = verified.claimString("sub")?.toLongOrNull()
            ?: throw HttpException(NetonErrorCode.INVALID_TOKEN, "INVALID_REFRESH_TOKEN_SUBJECT")
        val sessionVersion = MemberTable.get(memberId)?.sessionVersion ?: 0L
        val tokenSid = verified.claimString("sid")?.toLongOrNull() ?: 0L
        if (tokenSid != sessionVersion) {
            throw HttpException(NetonErrorCode.REFRESH_TOKEN_EXPIRED, "REFRESH_TOKEN_EXPIRED")
        }
        val deviceId = command.deviceId?.takeIf { it.isNotBlank() } ?: mintDeviceId()
        return buildBundle(memberId, deviceId, sessionVersion, deviceCreated = false)
    }

    override suspend fun onPasswordChanged(event: MemberPasswordChangedEvent) {
        // 原子自增，不能读出来再写回去：并发改密会互相覆盖，旧 token 就失效不掉
        MemberTable.query { where { Member::id eq event.memberId } }
            .update { increment(Member::sessionVersion) }
    }

    private fun buildBundle(
        memberId: Long, deviceId: String, sessionVersion: Long, deviceCreated: Boolean,
    ): MemberTokenBundle {
        val uid = UserId(memberId.toULong())
        val access = jwt.createToken(
            userId = uid, expiresInSeconds = ACCESS_TTL_SECONDS,
            extraClaims = mapOf("type" to "access", "sid" to sessionVersion, "did" to deviceId, "scope" to "user"),
        )
        val refresh = jwt.createToken(
            userId = uid, expiresInSeconds = REFRESH_TTL_SECONDS,
            extraClaims = mapOf("type" to "refresh", "sid" to sessionVersion, "did" to deviceId),
        )
        return MemberTokenBundle(
            userId = memberId, accessToken = access, refreshToken = refresh,
            expiresIn = ACCESS_TTL_SECONDS, refreshExpiresIn = REFRESH_TTL_SECONDS,
            deviceId = deviceId, sessionVersion = sessionVersion, deviceCreated = deviceCreated,
            scope = SCOPE, issuer = ISSUER,
        )
    }

    @OptIn(ExperimentalTime::class)
    private fun mintDeviceId(): String =
        "d-${Clock.System.now().toEpochMilliseconds().toString(36)}-${Random.nextInt(0, 0x1000000).toString(36)}"
}
