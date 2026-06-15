package controller.app.member

import com.netonstream.privchat.application.module.privchat.hook.HookBus
import com.netonstream.privchat.application.module.privchat.hook.hooks.MemberMobileChangedHook
import com.netonstream.privchat.application.module.privchat.hook.hooks.MemberPasswordChangedHook
import port.MemberIdentityAdapter
import port.MemberMobileChangedEvent
import port.MemberPasswordChangedEvent
import controller.app.member.dto.ResetMemberPasswordRequest
import controller.app.member.dto.UpdateMemberAvatarRequest
import controller.app.member.dto.UpdateMemberBioRequest
import controller.app.member.dto.UpdateMemberBirthdayRequest
import controller.app.member.dto.UpdateMemberGenderRequest
import controller.app.member.dto.UpdateMemberMobileRequest
import controller.app.member.dto.UpdateMemberNicknameRequest
import controller.app.member.dto.UpdateMemberPasswordRequest
import controller.app.member.dto.UpdateMemberUsernameRequest
import controller.app.member.dto.UpdateMemberUsernameResponse
import enums.SmsScene
import kotlinx.serialization.Serializable
import logic.MemberAuthLogic
import logic.MemberLogic
import logic.MemberProfileLogic
import model.Member
import neton.core.annotations.*
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.core.interfaces.Identity
import neton.database.dsl.*
import neton.redis.RedisClient
import neton.security.password.PasswordHasher
import neton.validation.annotations.Min
import table.MemberTable

@Serializable
data class AuthedSendSmsCodeRequest(
    /** 修改手机时传新手机号；修改密码时可省略（从登录态读取） */
    val mobile: String? = null,

    @property:Min(1)
    val scene: Int
)

/**
 * Member 用户面 profile 控制器（spec MODULE_MEMBER_PROFILE_SPEC §3 / §4）。
 *
 * 历史 `PUT /app/member/user/update`（混改 nickname + avatar）已下线，所有 profile
 * 字段按操作单一职责拆开，每个端点 publish 一次 [com.netonstream.privchat
 * .application.module.privchat.hook.hooks.MemberProfileChangedHook]，订阅者
 * 决定是否同步到 server。
 */
@Controller("/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic,
    private val memberAuthLogic: MemberAuthLogic,
    private val memberProfileLogic: MemberProfileLogic,
    private val redis: RedisClient? = null,
    private val hookBus: HookBus? = null,
    // MEMBER-IDENTITY-ADAPTER C2: mobile/password 变更回调走 adapter(与 hookBus 暂并存,HookBus 在 C5 移除)。
    private val identityAdapter: MemberIdentityAdapter? = null,
) {

    @Get("/get")
    suspend fun get(identity: Identity): Member? {
        val userId = identity.id.toLong()
        return memberLogic.get(userId)
    }

    // ──────────── profile 拆分端点（spec §4） ────────────

    @Put("/update-nickname")
    suspend fun updateNickname(
        identity: Identity,
        @Body request: UpdateMemberNicknameRequest,
    ) {
        memberProfileLogic.updateNickname(identity.id.toLong(), request.nickname)
    }

    @Put("/update-avatar")
    suspend fun updateAvatar(
        identity: Identity,
        @Body request: UpdateMemberAvatarRequest,
    ) {
        memberProfileLogic.updateAvatar(identity.id.toLong(), request.fileId)
    }

    @Put("/update-username")
    @FreshAuth
    suspend fun updateUsername(
        identity: Identity,
        @Body request: UpdateMemberUsernameRequest,
    ): UpdateMemberUsernameResponse {
        val result = memberProfileLogic.updateUsername(identity.id.toLong(), request.username)
        return UpdateMemberUsernameResponse(
            username = result.member.username
                ?: error("updateUsername returned member without username; logic invariant broken"),
            nextChangeAvailableAt = result.nextChangeAvailableAt,
        )
    }

    @Put("/update-bio")
    suspend fun updateBio(
        identity: Identity,
        @Body request: UpdateMemberBioRequest,
    ) {
        memberProfileLogic.updateBio(identity.id.toLong(), request.bio)
    }

    @Put("/update-gender")
    suspend fun updateGender(
        identity: Identity,
        @Body request: UpdateMemberGenderRequest,
    ) {
        memberProfileLogic.updateGender(identity.id.toLong(), request.gender)
    }

    @Put("/update-birthday")
    suspend fun updateBirthday(
        identity: Identity,
        @Body request: UpdateMemberBirthdayRequest,
    ) {
        memberProfileLogic.updateBirthday(identity.id.toLong(), request.birthday)
    }

    // ──────────── mobile / password / SMS（spec §4.8 已有，行为不变） ────────────

    @Put("/update-mobile")
    @FreshAuth
    suspend fun updateMobile(identity: Identity, @Body request: UpdateMemberMobileRequest) {
        val userId = identity.id.toLong()
        val mobile = request.mobile
        val smsCode = request.smsCode
        // Verify SMS code
        val storedCode = redis?.getValue("sms:code:$mobile")
        if (storedCode == null) {
            throw BadRequestException("SMS code expired or not sent")
        }
        if (storedCode != smsCode) {
            throw BadRequestException("Invalid SMS code")
        }
        redis.delete("sms:code:$mobile")

        val member = memberLogic.get(userId)
            ?: throw NotFoundException("Member not found: $userId")
        val oldMobile = member.mobile
        memberLogic.update(member.copy(mobile = mobile))
        // 跨模块通知：手机号变更（spec HOOK_REGISTRY §3.1）
        hookBus?.publish(MemberMobileChangedHook(uid = userId, oldMobile = oldMobile, newMobile = mobile))
        identityAdapter?.onMobileChanged(
            MemberMobileChangedEvent(memberId = userId, newMobile = mobile, oldMobile = oldMobile),
        )
    }

    @Put("/update-password")
    @FreshAuth
    suspend fun updatePassword(identity: Identity, @Body request: UpdateMemberPasswordRequest) {
        val userId = identity.id.toLong()
        val member = memberLogic.get(userId)
            ?: throw NotFoundException("Member not found: $userId")

        // Verify old password
        if (member.password != null) {
            val verification = PasswordHasher.verify(request.oldPassword, member.password)
            if (!verification.verified) {
                throw BadRequestException("Old password is incorrect")
            }
        }

        // Hash new password and update
        memberLogic.update(member.copy(password = PasswordHasher.hash(request.newPassword)))
        // 跨模块通知：密码变更，订阅者（如 :main）可调 server bumpSessions 让旧 IM token 失效（spec §4.2）
        hookBus?.publish(MemberPasswordChangedHook(uid = userId))
        identityAdapter?.onPasswordChanged(MemberPasswordChangedEvent(memberId = userId))
    }

    @Put("/reset-password")
    suspend fun resetPassword(@Body request: ResetMemberPasswordRequest) {
        val mobile = request.mobile
        val smsCode = request.smsCode
        // Verify SMS code
        val storedCode = redis?.getValue("sms:code:$mobile")
        if (storedCode == null) {
            throw BadRequestException("SMS code expired or not sent")
        }
        if (storedCode != smsCode) {
            throw BadRequestException("Invalid SMS code")
        }
        redis.delete("sms:code:$mobile")

        val member = MemberTable.oneWhere {
            Member::mobile eq mobile
        } ?: throw NotFoundException("Member not found with mobile: $mobile")

        // Hash new password and update
        memberLogic.update(member.copy(password = PasswordHasher.hash(request.newPassword)))
        hookBus?.publish(MemberPasswordChangedHook(uid = member.id))
        identityAdapter?.onPasswordChanged(MemberPasswordChangedEvent(memberId = member.id))
    }

    /**
     * 认证场景发送验证码：修改手机（scene=2）、修改密码（scene=3）。
     *
     * - 修改手机：[mobile] 为新手机号，校验未被其他用户占用。
     * - 修改密码：[mobile] 可省略，从当前登录用户的手机号自动获取。
     */
    @Post("/send-sms-code")
    @RateLimit(windowSeconds = 60, maxRequests = 5, scope = RateLimitScope.IP, message = "SMS code sending limit exceeded, please try again later")
    suspend fun sendSmsCode(identity: Identity, @Body request: AuthedSendSmsCodeRequest) {
        val scene = try {
            SmsScene.fromScene(request.scene)
        } catch (_: IllegalArgumentException) {
            throw BadRequestException("Invalid scene: ${request.scene}")
        }
        if (scene !in setOf(SmsScene.MEMBER_UPDATE_MOBILE, SmsScene.MEMBER_UPDATE_PASSWORD)) {
            throw BadRequestException("Scene ${request.scene} is not allowed here, use /auth/send-sms-code for login/reset-password")
        }

        val userId = identity.id.toLong()

        val mobile = when (scene) {
            SmsScene.MEMBER_UPDATE_PASSWORD -> {
                // 修改密码：手机号来自登录用户自己的档案，忽略请求中的 mobile
                memberLogic.get(userId)?.mobile
                    ?: throw BadRequestException("No mobile bound to current account")
            }
            SmsScene.MEMBER_UPDATE_MOBILE -> {
                // 修改手机：手机号为新号，由请求提供（必须 E.164）
                request.mobile?.takeIf { it.matches(Regex(controller.app.auth.dto.E164_REGEX)) }
                    ?: throw BadRequestException(controller.app.auth.dto.E164_MESSAGE)
            }
            else -> throw BadRequestException("Unexpected scene: $scene")
        }

        memberAuthLogic.sendSmsCode(mobile, scene)
    }
}
