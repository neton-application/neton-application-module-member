package controller.app.member

import controller.app.member.dto.ResetMemberPasswordRequest
import controller.app.member.dto.UpdateMemberMobileRequest
import controller.app.member.dto.UpdateMemberPasswordRequest
import controller.app.member.dto.UpdateMemberProfileRequest
import logic.MemberLogic
import model.Member
import neton.core.annotations.*
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import neton.core.interfaces.Identity
import neton.database.dsl.*
import neton.redis.RedisClient
import neton.security.password.PasswordHasher
import table.MemberTable

@Controller("/app/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic,
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
}
