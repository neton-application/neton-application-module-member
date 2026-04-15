package controller.app.member

import controller.app.member.dto.ResetMemberPasswordRequest
import controller.app.member.dto.UpdateMemberMobileRequest
import controller.app.member.dto.UpdateMemberPasswordRequest
import controller.app.member.dto.UpdateMemberProfileRequest
import enums.SmsScene
import kotlinx.serialization.Serializable
import logic.MemberAuthLogic
import logic.MemberLogic
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

@Controller("/app/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic,
    private val memberAuthLogic: MemberAuthLogic,
    private val redis: RedisClient? = null
) {

    @Get("/get")
    suspend fun get(identity: Identity): Member? {
        val userId = identity.id.toLong()
        return memberLogic.get(userId)
    }

    @Put("/update")
    suspend fun update(identity: Identity, @Body request: UpdateMemberProfileRequest) {
        val userId = identity.id.toLong()
        val member = memberLogic.get(userId)
            ?: throw NotFoundException("Member not found: $userId")
        memberLogic.update(
            member.copy(
                nickname = request.nickname,
                avatar = request.avatar,
            )
        )
    }

    @Put("/update-mobile")
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
        memberLogic.update(member.copy(mobile = mobile))
    }

    @Put("/update-password")
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
                // 修改手机：手机号为新号，由请求提供
                request.mobile?.takeIf { it.matches(Regex("^1\\d{10}$")) }
                    ?: throw BadRequestException("A valid new mobile number is required for this scene")
            }
            else -> throw BadRequestException("Unexpected scene: $scene")
        }

        memberAuthLogic.sendSmsCode(mobile, scene)
    }
}
