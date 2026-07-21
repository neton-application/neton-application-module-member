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
import neton.database.api.DbContext
import neton.database.dsl.*
import neton.security.identity.UserId
import neton.security.jwt.JwtAuthenticator
import kotlin.random.Random
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 内置身份后端（自托管账号，无外部 server）。
 *
 * - 账号：按 mobile/username 命中 member_users 复用 id，否则本地 [MemberIdGenerator] 生成。
 * - token：复用应用的 [JwtAuthenticator]（HS256 + security.jwt.secretKey），与 admin token
 *   同一套签验，`/app` 路由组直接验通，`Identity.id = sub`。
 * - 会话：token 携带 `sid`(session_version)/`did`(device)；改密 bump session_version。
 *   v1 仅签发携带 + 刷新处校验；接口访问强校验（登出全端即时失效）留 v2 的 member_sessions guard。
 *
 * privchat 模式由 PrivchatMemberIdentityAdapter override（走 privchat-server）。
 */
class BuiltinMemberIdentityAdapter(
    private val jwt: JwtAuthenticator,
    private val idGenerator: MemberIdGenerator,
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
        return MemberAccountRef(MemberIdentityProvider.BUILTIN, idGenerator.next(), created = true)
    }

    override suspend fun issueToken(command: IssueMemberTokenCommand): MemberTokenBundle {
        val sessionVersion = MemberTable.get(command.memberId)?.sessionVersion ?: 0L
        val deviceId = command.deviceId?.takeIf { it.isNotBlank() } ?: mintDeviceId()
        return buildBundle(command.memberId, deviceId, sessionVersion, deviceCreated = command.deviceId.isNullOrBlank())
    }

    override suspend fun refreshToken(command: RefreshMemberTokenCommand): MemberTokenBundle {
        val verified = try {
            jwt.verifyToken(command.refreshToken)
        } catch (e: Exception) {
            throw IllegalArgumentException("invalid refresh token")
        }
        if (verified.claimString("type") != "refresh") throw IllegalArgumentException("not a refresh token")
        val memberId = verified.claimString("sub")?.toLongOrNull()
            ?: throw IllegalArgumentException("invalid refresh token subject")
        val sessionVersion = MemberTable.get(memberId)?.sessionVersion ?: 0L
        val tokenSid = verified.claimString("sid")?.toLongOrNull() ?: 0L
        if (tokenSid != sessionVersion) throw IllegalArgumentException("session expired")
        val deviceId = command.deviceId?.takeIf { it.isNotBlank() } ?: mintDeviceId()
        return buildBundle(memberId, deviceId, sessionVersion, deviceCreated = false)
    }

    override suspend fun onPasswordChanged(event: MemberPasswordChangedEvent) {
        db.execute(
            "UPDATE member_users SET session_version = session_version + 1 WHERE id = :id",
            mapOf("id" to event.memberId),
        )
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
