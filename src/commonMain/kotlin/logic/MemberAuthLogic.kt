package logic

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.client.dto.CreateUserRequest
import com.netonstream.privchat.application.module.privchat.client.dto.DeviceInfoInput
import com.netonstream.privchat.application.module.privchat.client.dto.IssueImTokenRequest
import com.netonstream.privchat.application.module.privchat.client.dto.IssueImTokenResponse
import controller.admin.auth.dto.LoginDeviceInfo
import enums.SmsScene
import model.Member
import table.MemberTable
import controller.app.auth.dto.MemberLoginRequest
import controller.app.auth.dto.MemberLoginResponse
import neton.database.dsl.*

import neton.security.identity.UserId
import neton.logging.Logger
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.security.jwt.JwtAuthenticatorV1
import neton.redis.RedisClient
import neton.security.identity.AuthenticationException
import neton.security.password.PasswordHasher
import kotlin.random.Random
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock

class MemberAuthLogic(
    private val log: Logger,
    private val privchatService: PrivchatServiceClient,
    private val jwt: JwtAuthenticatorV1? = null,
    private val redis: RedisClient? = null,
    private val messageSendLogic: MessageSendLogic? = null,
    private val socialUserLogic: SocialUserLogic? = null,
) {

    companion object {
        private const val ACCESS_TOKEN_EXPIRES = 7200L  // 2 hours
        private const val REFRESH_TOKEN_EXPIRES = 604800L  // 7 days
        private const val SMS_CODE_PREFIX = "sms:code:"
        private const val SMS_CODE_TTL_SECONDS = 300L  // 5 minutes
        private const val DEFAULT_PLATFORM = "unknown"
        private const val DEVICE_CLAIM = "device_id"
    }

    private fun maskMobile(mobile: String): String {
        return if (mobile.length < 7) "***" else "${mobile.take(3)}****${mobile.takeLast(4)}"
    }

    suspend fun login(request: MemberLoginRequest): MemberLoginResponse {
        var member = MemberTable.oneWhere {
            Member::mobile eq request.mobile
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

        val imToken = issueImTokenSafely(member.id, request.device)
        log.info("member.login.success", mapOf("userId" to member.id, "mobile" to maskMobile(request.mobile)))

        return buildResponse(member.id, imToken)
    }

    suspend fun smsLogin(request: MemberLoginRequest): MemberLoginResponse {
        val smsCode = request.smsCode ?: throw BadRequestException("SMS code is required")
        verifySmsCode(request.mobile, smsCode)

        val member = MemberTable.oneWhere { Member::mobile eq request.mobile }
            ?: registerFromPrivchat(request.mobile)

        if (member.status == 0) {
            throw BadRequestException("Account is disabled")
        }

        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        val imToken = issueImTokenSafely(member.id, request.device)
        log.info("member.sms.login.success", mapOf("userId" to member.id, "mobile" to maskMobile(request.mobile)))

        return buildResponse(member.id, imToken)
    }

    suspend fun logout(userId: Long) {
        // Blacklist the user's tokens
        redis?.set("auth:member:blacklist:$userId", "1", ttl = ACCESS_TOKEN_EXPIRES.seconds)
        log.info("member.logout", mapOf("userId" to userId))
    }

    suspend fun refreshToken(refreshToken: String, requestDeviceId: String?): MemberLoginResponse {
        val jwtInstance = jwt ?: throw BadRequestException("JWT service not available")
        val verifiedToken = try {
            jwtInstance.verifyToken(refreshToken)
        } catch (_: AuthenticationException) {
            throw BadRequestException("Invalid or expired refresh token")
        }
        if (verifiedToken.claimString("type") != "refresh" || verifiedToken.claimString("scope") != "member") {
            throw BadRequestException("Invalid or expired refresh token")
        }
        val claimDeviceId = verifiedToken.claimString(DEVICE_CLAIM)
        if (claimDeviceId != null && requestDeviceId != null && claimDeviceId != requestDeviceId) {
            // 跨设备复用 refresh token：拒
            log.info(
                "member.token.refresh.device_mismatch",
                mapOf("claim" to claimDeviceId, "request" to requestDeviceId)
            )
            throw BadRequestException("Refresh token does not match the requesting device")
        }
        val userId = verifiedToken.identity.userId.value.toLong()

        val member = MemberTable.get(userId)
            ?: throw NotFoundException("Member not found")

        if (member.status == 0) {
            throw BadRequestException("Account is disabled")
        }

        log.info("member.token.refreshed", mapOf("userId" to userId))

        // refresh 路径不重发 IM token：server 端有内置 refresh RPC（spec TOKEN_API §3.3 v1.3），
        // 客户端持有 im_refresh_token 直接走 server。此处仅续 member token，沿用旧 deviceId。
        return buildResponse(
            uid = member.id,
            imToken = null,
            deviceIdClaim = claimDeviceId ?: requestDeviceId,
        )
    }

    suspend fun sendSmsCode(mobile: String, scene: SmsScene) {
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

    suspend fun validateSmsCode(mobile: String, code: String): Boolean {
        return try {
            verifySmsCode(mobile, code)
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

        val imToken = issueImTokenSafely(member.id, device)
        log.info("member.social.login.success", mapOf("userId" to member.id, "socialType" to socialType))
        return buildResponse(member.id, imToken)
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

    private suspend fun issueImTokenSafely(uid: Long, device: LoginDeviceInfo?): IssueImTokenResponse {
        val req = IssueImTokenRequest(
            deviceId = device?.deviceId,
            deviceInfo = DeviceInfoInput(
                appId = device?.platform ?: DEFAULT_PLATFORM,
                deviceName = device?.deviceName ?: "",
                deviceModel = device?.deviceModel ?: "",
                osVersion = device?.osVersion ?: "",
                appVersion = device?.appVersion ?: "",
                ipAddress = device?.ipAddress ?: "",
            ),
        )
        return privchatService.issueImToken(uid, req)
    }

    private fun buildResponse(
        uid: Long,
        imToken: IssueImTokenResponse?,
        deviceIdClaim: String? = imToken?.deviceId,
    ): MemberLoginResponse {
        val accessToken = generateAccessToken(uid, deviceIdClaim)
        val refreshToken = generateRefreshToken(uid, deviceIdClaim)
        return MemberLoginResponse(
            userId = uid,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = ACCESS_TOKEN_EXPIRES,
            imToken = imToken?.imToken,
            imRefreshToken = imToken?.imRefreshToken?.takeIf { it.isNotEmpty() },
            imRefreshExpiresIn = imToken?.imRefreshExpiresIn?.takeIf { it > 0 },
            imDeviceId = imToken?.deviceId,
            imExpiresIn = imToken?.expiresIn,
            sessionVersion = imToken?.sessionVersion,
            deviceCreated = imToken?.deviceCreated,
        )
    }

    private fun generateAccessToken(userId: Long, deviceId: String?): String {
        val jwtInstance = jwt ?: throw IllegalStateException("JWT is not configured — set security.jwt.secretKey")
        val extra = mutableMapOf<String, String>(
            "type" to "access",
            "scope" to "member",
        )
        deviceId?.let { extra[DEVICE_CLAIM] = it }
        return jwtInstance.createToken(
            userId = UserId(userId.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = extra,
        )
    }

    private fun generateRefreshToken(userId: Long, deviceId: String?): String {
        val jwtInstance = jwt ?: throw IllegalStateException("JWT is not configured — set security.jwt.secretKey")
        val extra = mutableMapOf<String, String>(
            "type" to "refresh",
            "scope" to "member",
        )
        deviceId?.let { extra[DEVICE_CLAIM] = it }
        return jwtInstance.createToken(
            userId = UserId(userId.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = extra,
        )
    }
}
