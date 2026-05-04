package logic

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.client.dto.CreateUserRequest
import com.netonstream.privchat.application.module.privchat.client.dto.DeviceInfoInput
import com.netonstream.privchat.application.module.privchat.client.dto.IssueAuthTokenRequest
import com.netonstream.privchat.application.module.privchat.client.dto.UnifiedLoginResponse
import controller.admin.auth.dto.LoginDeviceInfo
import controller.app.auth.dto.E164_MESSAGE
import controller.app.auth.dto.E164_REGEX
import enums.SmsScene
import model.Member
import table.MemberTable
import controller.app.auth.dto.MemberLoginRequest
import controller.app.auth.dto.MemberLoginResponse
import neton.database.dsl.*

import neton.logging.Logger
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.redis.RedisClient
import neton.security.password.PasswordHasher
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock

class MemberAuthLogic(
    private val log: Logger,
    private val privchatService: PrivchatServiceClient,
    private val redis: RedisClient? = null,
    private val messageSendLogic: MessageSendLogic? = null,
    private val socialUserLogic: SocialUserLogic? = null,
) {

    companion object {
        private const val SMS_CODE_PREFIX = "sms:code:"
        private const val SMS_CODE_TTL_SECONDS = 300L  // 5 minutes
        private const val DEFAULT_PLATFORM = "unknown"
        // logout 黑名单 TTL：与 server unified token 默认 access TTL 对齐（1h）
        private const val LOGOUT_BLACKLIST_TTL_SECONDS = 3600L

        private val E164 = Regex(E164_REGEX)
    }

    /**
     * 复核并归一手机号：仅去首尾空格，**不**自动 prepend `+`、**不**做国家级深度校验。
     * 严格要求 E.164（`+` + 8-15 位数字）；非法 → BadRequest INVALID_PHONE_FORMAT。
     *
     * neton-validation 在多模块 `bindIfAbsent` 模式下后注册的 validator 会被静默忽略
     * （framework 层 bug），此处兜底确保不正确格式的手机号无法进入认证流程。
     */
    private fun requireE164(mobile: String): String {
        val trimmed = mobile.trim()
        if (!E164.matches(trimmed)) throw BadRequestException(E164_MESSAGE)
        return trimmed
    }

    private fun maskMobile(mobile: String): String {
        // E.164 形如 `+8615000000000`：保留 `+` 和国家码部分 + 末 4 位。
        return if (mobile.length < 8) "***" else "${mobile.take(4)}****${mobile.takeLast(4)}"
    }

    suspend fun login(request: MemberLoginRequest): MemberLoginResponse {
        val mobile = requireE164(request.mobile)
        var member = MemberTable.oneWhere {
            Member::mobile eq mobile
        } ?: throw BadRequestException("Invalid mobile or password")

        if (member.password == null) {
            throw BadRequestException("Password not set for this account")
        }

        val password = request.password ?: ""
        val storedPassword = member.password
        val passwordVerification = PasswordHasher.verify(password, storedPassword)
        if (!passwordVerification.verified) {
            throw BadRequestException("Invalid mobile or password")
        }
        if (passwordVerification.needsRehash) {
            member = member.copy(password = PasswordHasher.hash(password))
            MemberTable.update(member)
        }

        if (member.status == 0) {
            throw BadRequestException("Account is disabled")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("member.login.success", mapOf("userId" to member.id, "mobile" to maskMobile(mobile)))
        return buildResponse(member.id, request.device)
    }

    suspend fun smsLogin(request: MemberLoginRequest): MemberLoginResponse {
        val mobile = requireE164(request.mobile)
        val smsCode = request.smsCode ?: throw BadRequestException("SMS code is required")
        verifySmsCode(mobile, smsCode)

        val member = MemberTable.oneWhere { Member::mobile eq mobile }
            ?: registerFromPrivchat(mobile)

        if (member.status == 0) {
            throw BadRequestException("Account is disabled")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("member.sms.login.success", mapOf("userId" to member.id, "mobile" to maskMobile(mobile)))
        return buildResponse(member.id, request.device)
    }

    suspend fun logout(userId: Long) {
        // Blacklist the user's tokens
        redis?.set("auth:member:blacklist:$userId", "1", ttl = LOGOUT_BLACKLIST_TTL_SECONDS.seconds)
        log.info("member.logout", mapOf("userId" to userId))
    }

    /**
     * 代理到 server `/api/service/auth/refresh`（spec §6.1）。token 验证、device_id 匹配、
     * session_version 校验全部由 server 完成；application 不再持有自签 refresh token。
     */
    suspend fun refreshToken(refreshToken: String, requestDeviceId: String?): MemberLoginResponse {
        val deviceId = requestDeviceId?.takeIf { it.isNotBlank() }
            ?: throw BadRequestException("device_id is required for refresh")
        val unified = privchatService.refreshAuthToken(refreshToken = refreshToken, deviceId = deviceId)
        log.info("member.token.refreshed", mapOf("userId" to unified.userId))
        return unified.toMemberLoginResponse()
    }

    suspend fun sendSmsCode(rawMobile: String, scene: SmsScene) {
        val mobile = requireE164(rawMobile)
        // Scene-specific pre-validation
        when (scene) {
            SmsScene.MEMBER_UPDATE_MOBILE -> {
                // 修改手机：新手机号不能已被其他用户使用
                val existing = MemberTable.oneWhere { Member::mobile eq mobile }
                if (existing != null) {
                    throw BadRequestException("Mobile number is already in use")
                }
            }
            SmsScene.MEMBER_RESET_PASSWORD,
            SmsScene.MEMBER_UPDATE_PASSWORD -> {
                // 重置/修改密码：手机号必须已注册
                MemberTable.oneWhere { Member::mobile eq mobile }
                    ?: throw BadRequestException("Mobile number is not registered")
            }
            SmsScene.MEMBER_LOGIN -> Unit // 登录：无需前置校验，允许自动注册
        }

        if (messageSendLogic != null) {
            messageSendLogic.sendVerificationCode(mobile, scene.templateCode)
        } else {
            // Fallback: store code directly in Redis when SMS channel is not configured.
            // Random.Default maps to arc4random on Native (cryptographically secure).
            val code = Random.Default.nextInt(100000, 1000000).toString()
            redis?.set("$SMS_CODE_PREFIX$mobile", code, ttl = SMS_CODE_TTL_SECONDS.seconds)
            log.info("member.sms.code.generated", mapOf("mobile" to maskMobile(mobile), "scene" to scene.name))
        }
    }

    suspend fun validateSmsCode(rawMobile: String, code: String): Boolean {
        return try {
            verifySmsCode(requireE164(rawMobile), code)
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- Social login ---

    suspend fun socialAuthRedirect(socialType: String, redirectUri: String): String {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")
        return social.getAuthRedirectUrl(socialType, redirectUri)
    }

    suspend fun socialLogin(
        socialType: String,
        code: String,
        redirectUri: String,
        device: LoginDeviceInfo?,
    ): MemberLoginResponse {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")

        val socialUser = social.socialLogin(socialType, code, redirectUri, userType = 2)

        val member: Member
        if (socialUser.userId == 0L) {
            // 走 server 取 uid，再写镜像（spec UPSTREAM §5：所有 fork 注册分支必须先 server 分配 uid）
            val created = privchatService.createUser(
                CreateUserRequest(
                    username = socialUser.openId.takeIf { it.isNotEmpty() },
                    displayName = socialUser.nickname,
                    avatarUrl = socialUser.avatar,
                )
            )
            val newMember = Member(
                id = created.userId,
                nickname = socialUser.nickname ?: "Member_${socialUser.openId.take(6)}",
                avatar = socialUser.avatar,
            )
            member = insertMemberWithProvidedId(newMember)
            social.bind(member.id, userType = 2, socialType, code, redirectUri)
            log.info("member.social.auto_register", mapOf("userId" to member.id, "socialType" to socialType))
        } else {
            member = MemberTable.get(socialUser.userId)
                ?: throw NotFoundException("Bound member not found")
        }

        if (member.status == 0) {
            throw BadRequestException("Account is disabled")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("member.social.login.success", mapOf("userId" to member.id, "socialType" to socialType))
        return buildResponse(member.id, device)
    }

    // --- Private helpers ---

    private suspend fun verifySmsCode(mobile: String, code: String) {
        val redisInstance = redis ?: throw BadRequestException("SMS code service unavailable")
        val key = "$SMS_CODE_PREFIX$mobile"

        // Atomically verify and consume the SMS code via Lua script to prevent TOCTOU.
        // Returns: 0 = key not found, 1 = wrong code, 2 = matched and deleted.
        val script = """
            local val = redis.call('GET', KEYS[1])
            if val == false then return 0 end
            if val == ARGV[1] then
                redis.call('DEL', KEYS[1])
                return 2
            end
            return 1
        """.trimIndent()

        when (redisInstance.evalToLong(script, listOf(key), listOf(code))) {
            0L -> throw BadRequestException("SMS code expired or not sent")
            1L -> throw BadRequestException("Invalid SMS code")
            // 2L = matched and consumed; fall through
        }
    }

    /**
     * SMS 自动注册：先看 server 是否已有该手机号，命中则复用 uid，否则 createUser。
     * 写本地镜像用 [insertMemberWithProvidedId]（caller-provided id）。
     *
     * 入口已要求 E.164（`E164_REGEX`），这里直接透传给 server。
     */
    private suspend fun registerFromPrivchat(mobile: String): Member {
        val remoteUser = privchatService.getUserByMobile(mobile)
        val uid = remoteUser?.userId
            ?: privchatService.createUser(CreateUserRequest(phone = mobile)).userId
        val newMember = Member(
            id = uid,
            mobile = mobile,
            nickname = "Member_${mobile.takeLast(4)}",
        )
        log.info(
            "member.sms.auto_register",
            mapOf("userId" to uid, "mobile" to maskMobile(mobile), "remoteHit" to (remoteUser != null))
        )
        return insertMemberWithProvidedId(newMember)
    }

    /**
     * 给指定 uid + 设备直接颁发完整的登录返回（spec QR_API §5）。
     *
     * 用途：扫码登录确认时，application 已经通过 mobile 的 member token 拿到 scanner_uid，
     * 还需要给 **Web 端** 设备签发 token 推回 Web 的 unauth 连接。
     */
    suspend fun issueLoginResponseForUid(
        uid: Long,
        device: LoginDeviceInfo?,
    ): MemberLoginResponse = buildResponse(uid, device)

    /**
     * 调 server `/api/service/auth/issue` 颁发 unified token，把 [UnifiedLoginResponse] 映射成
     * 客户端可见的 [MemberLoginResponse]（spec §8）。
     */
    private suspend fun buildResponse(uid: Long, device: LoginDeviceInfo?): MemberLoginResponse {
        // server `DeviceInfo` 字段全部 NOT NULL；客户端缺省时给 "unknown" 占位（spec TOKEN_API §3.2）。
        val req = IssueAuthTokenRequest(
            userId = uid,
            deviceId = device?.deviceId?.takeIf { it.isNotBlank() },
            deviceInfo = DeviceInfoInput(
                appId = device?.platform?.takeIf { it.isNotBlank() } ?: DEFAULT_PLATFORM,
                deviceName = device?.deviceName?.takeIf { it.isNotBlank() } ?: "unknown",
                deviceModel = device?.deviceModel?.takeIf { it.isNotBlank() } ?: "unknown",
                osVersion = device?.osVersion?.takeIf { it.isNotBlank() } ?: "unknown",
                appVersion = device?.appVersion?.takeIf { it.isNotBlank() } ?: "unknown",
                ipAddress = device?.ipAddress?.takeIf { it.isNotBlank() } ?: "0.0.0.0",
            ),
            scope = listOf("user"),
            audience = listOf("privchat-application", "privchat-server"),
            businessSystemId = "privchat-application",
        )
        return privchatService.issueAuthToken(req).toMemberLoginResponse()
    }

    private fun UnifiedLoginResponse.toMemberLoginResponse(): MemberLoginResponse =
        MemberLoginResponse(
            userId = userId,
            accessToken = accessToken,
            refreshToken = refreshToken,
            tokenType = tokenType,
            expiresIn = expiresIn,
            refreshExpiresIn = refreshExpiresIn,
            deviceId = deviceId,
            sessionVersion = sessionVersion,
            deviceCreated = deviceCreated,
            scope = scope,
            issuer = issuer,
        )
}
