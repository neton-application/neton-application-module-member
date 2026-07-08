package logic

import port.MemberIdentityAdapter
import port.CreateMemberAccountCommand
import port.IssueMemberTokenCommand
import port.RefreshMemberTokenCommand
import port.MemberTokenBundle
import controller.admin.auth.dto.LoginDeviceInfo
import controller.app.auth.dto.E164_MESSAGE
import controller.app.auth.dto.E164_REGEX
import controller.app.auth.dto.RequiredAction
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
    private val identityAdapter: MemberIdentityAdapter,
    private val requiredActionsLogic: RequiredActionsLogic,
    private val redis: RedisClient? = null,
    private val messageSendLogic: MessageSendLogic? = null,
    private val socialUserLogic: SocialUserLogic? = null,
    private val inviteLogic: MemberInviteLogic? = null,
    private val authPolicy: port.MemberAuthPolicy = port.MemberAuthPolicy(),
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
            ?: registerNewUser(
                mode = port.MemberAuthPolicy.MODE_PHONE_SMS,
                identifierMasked = maskMobile(mobile),
                inviteCode = request.inviteCode,
                buildMember = { registerViaAdapter(mobile, nickname = request.nickname) },
            )

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
        val bundle = identityAdapter.refreshToken(
            RefreshMemberTokenCommand(refreshToken = refreshToken, deviceId = deviceId),
        )
        log.info("member.token.refreshed", mapOf("userId" to bundle.userId))
        // R8.4a：refresh 路径也带最新 required actions（用户可能在另一端完成
        // 了 onboarding，本端 refresh 后应该立刻拿到空数组）。
        val actions = requiredActionsLogic.computeForUid(bundle.userId)
        return bundle.toMemberLoginResponse(actions)
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
            val ref = identityAdapter.createOrBindAccount(
                CreateMemberAccountCommand(
                    username = socialUser.openId.takeIf { it.isNotEmpty() },
                    displayName = socialUser.nickname,
                    avatarUrl = socialUser.avatar,
                )
            )
            val newMember = Member(
                id = ref.memberId,
                identityProvider = ref.provider,
                // 社交昵称缺失时留空，不自动编造（空昵称由 complete_profile 引导补齐）。
                nickname = socialUser.nickname ?: "",
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
    /**
     * 统一注册收敛(MEMBER_INVITE_CODE §5.3):所有注册方式共用——
     * 邀请码校验(悲观,失败=注册失败)→ 建用户 → 同事务计数+record → 提交后自动加好友。
     * `inviteCodeRequired` 只在这里生效(评审 B:存量用户登录不受影响)。
     */
    private suspend fun registerNewUser(
        mode: String,
        identifierMasked: String?,
        inviteCode: String?,
        buildMember: suspend () -> Member,
    ): Member {
        // inviteCodeRequired 不再在注册期拦截:改为登录后 required-actions
        // gate(bind_invite_code,见 RequiredActionsLogic)。带码注册仍走原路径。
        val trimmedCode = inviteCode?.trim()?.takeIf { it.isNotEmpty() }
        val codeEntity = trimmedCode?.let {
            val logic = inviteLogic ?: throw BadRequestException("INVITE_CODE_UNSUPPORTED")
            logic.validate(it)
        }
        val member = buildMember()
        if (codeEntity != null && inviteLogic != null) {
            val record = inviteLogic.applyInviteForNewUser(codeEntity, member.id, mode, identifierMasked)
            inviteLogic.dispatchAutoFriend(record.id)
        }
        return member
    }

    /** USERNAME_PASSWORD 注册(MEMBER_INVITE_CODE §5.1)。 */
    suspend fun register(request: controller.app.auth.dto.MemberRegisterRequest): MemberLoginResponse {
        if (request.mode != port.MemberAuthPolicy.MODE_USERNAME_PASSWORD) {
            throw BadRequestException("UNSUPPORTED_REGISTER_MODE")
        }
        if (port.MemberAuthPolicy.MODE_USERNAME_PASSWORD !in authPolicy.registerModes) {
            throw BadRequestException("REGISTER_MODE_DISABLED")
        }
        // username 规则 = MemberProfileLogic 同一真源(lowercase 归一 + 格式 + 保留字)
        val username = request.username.trim().lowercase()
        if (!MemberProfileLogic.USERNAME_REGEX.matches(username)) {
            throw BadRequestException("INVALID_USERNAME_FORMAT")
        }
        if (username in MemberProfileLogic.USERNAME_RESERVED) {
            throw BadRequestException("USERNAME_RESERVED")
        }
        if (request.password.length < 8) {
            throw BadRequestException("PASSWORD_TOO_SHORT")
        }
        // nickname 不在注册期强制:空昵称由 complete_profile required-action 引导补全
        if (MemberTable.oneWhere { Member::username eq username } != null) {
            throw BadRequestException("USERNAME_TAKEN")
        }
        val member = registerNewUser(
            mode = port.MemberAuthPolicy.MODE_USERNAME_PASSWORD,
            identifierMasked = maskUsername(username),
            inviteCode = request.inviteCode,
        ) {
            val ref = identityAdapter.createOrBindAccount(CreateMemberAccountCommand(username = username))
            insertMemberWithProvidedId(
                Member(
                    id = ref.memberId,
                    identityProvider = ref.provider,
                    username = username,
                    usernameUpdatedAt = Clock.System.now().toEpochMilliseconds(),
                    password = PasswordHasher.hash(request.password),
                    nickname = request.nickname?.trim().orEmpty(),
                ),
            ).also {
                log.info("member.register.username", mapOf("userId" to it.id, "username" to username))
            }
        }
        return buildResponse(member.id, request.device)
    }

    /** 账号密码登录(USERNAME_PASSWORD 注册的用户)。 */
    suspend fun usernameLogin(username: String, password: String, device: LoginDeviceInfo?): MemberLoginResponse {
        val normalized = username.trim().lowercase()
        // 凭证错误是认证失败(401/10011),不是参数错误(400/10100);message 用机器码,
        // 客户端按码映射本地化文案(用户拍板:错误必须人话,不泄原始异常)。
        var member = MemberTable.oneWhere { Member::username eq normalized }
            ?: throw neton.core.http.HttpException(neton.core.http.NetonErrorCode.INVALID_CREDENTIALS, "INVALID_CREDENTIALS")
        val stored = member.password
            ?: throw neton.core.http.HttpException(neton.core.http.NetonErrorCode.INVALID_CREDENTIALS, "INVALID_CREDENTIALS")
        val verification = PasswordHasher.verify(password, stored)
        if (!verification.verified) {
            throw neton.core.http.HttpException(neton.core.http.NetonErrorCode.INVALID_CREDENTIALS, "INVALID_CREDENTIALS")
        }
        if (verification.needsRehash) {
            member = member.copy(password = PasswordHasher.hash(password))
            MemberTable.update(member)
        }
        if (member.status == 0) {
            throw neton.core.http.HttpException(neton.core.http.NetonErrorCode.USER_BANNED, "ACCOUNT_DISABLED")
        }
        MemberTable.update(member.copy(loginDate = Clock.System.now().toEpochMilliseconds()))
        log.info("member.login.username.success", mapOf("userId" to member.id))
        return buildResponse(member.id, device)
    }

    private fun maskUsername(username: String): String =
        if (username.length <= 4) "${username.first()}***"
        else "${username.take(2)}***${username.takeLast(2)}"

    private suspend fun registerViaAdapter(mobile: String, nickname: String? = null): Member {
        val ref = identityAdapter.createOrBindAccount(CreateMemberAccountCommand(mobile = mobile))
        // 昵称:注册表单直填则落库(跳过 complete_profile);否则留空走首登引导。
        val newMember = Member(
            id = ref.memberId,
            identityProvider = ref.provider,
            mobile = mobile,
            nickname = nickname?.trim().orEmpty(),
        )
        if (!ref.created) {
            // 绑回了 server 既有账号但 platform 无 member 镜像：生产=手机号回收/换绑场景，
            // 测试环境=platform 与 IM 库不同步（半重置）。可观测告警，便于排查“新注册继承旧会话”。
            log.warn(
                "member.sms.rebind_existing_im_account",
                mapOf("userId" to ref.memberId, "mobile" to maskMobile(mobile))
            )
        }
        log.info(
            "member.sms.auto_register",
            mapOf("userId" to ref.memberId, "mobile" to maskMobile(mobile), "created" to ref.created)
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
        // device 信息透传给 adapter;具体后端(privchat)再映射成它的设备/令牌请求。
        val bundle = identityAdapter.issueToken(
            IssueMemberTokenCommand(
                memberId = uid,
                deviceId = device?.deviceId?.takeIf { it.isNotBlank() },
                platform = device?.platform?.takeIf { it.isNotBlank() },
                deviceName = device?.deviceName?.takeIf { it.isNotBlank() },
                deviceModel = device?.deviceModel?.takeIf { it.isNotBlank() },
                osVersion = device?.osVersion?.takeIf { it.isNotBlank() },
                appVersion = device?.appVersion?.takeIf { it.isNotBlank() },
                ipAddress = device?.ipAddress?.takeIf { it.isNotBlank() },
            ),
        )
        // R8.4a：注入 Post-login Required Actions。token 签发不感知它们;application 层计算后随响应下发。
        val actions = requiredActionsLogic.computeForUid(uid)
        return bundle.toMemberLoginResponse(actions)
    }

    private fun MemberTokenBundle.toMemberLoginResponse(
        requiredActions: List<RequiredAction>,
    ): MemberLoginResponse =
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
            requiredActions = requiredActions,
        )
}
