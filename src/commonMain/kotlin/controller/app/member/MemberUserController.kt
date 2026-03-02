package controller.app.member

import logic.MemberLogic
import model.Member
import table.MemberTable
import neton.database.dsl.*
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put
import neton.core.annotations.Body
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import infra.PasswordEncoder
import neton.redis.RedisClient

@Controller("/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic,
    private val redis: RedisClient? = null
) {

    @Get("/get")
    suspend fun get(userId: Long): Member? {
        return memberLogic.get(userId)
    }

    @Put("/update")
    suspend fun update(@Body member: Member) {
        memberLogic.update(member)
    }

    @Put("/update-mobile")
    suspend fun updateMobile(userId: Long, mobile: String, smsCode: String) {
        // Verify SMS code
        val storedCode = redis?.getValue("sms:code:$mobile")
        if (storedCode == null) {
            throw BadRequestException("SMS code expired or not sent")
        }
        if (storedCode != smsCode) {
            throw BadRequestException("Invalid SMS code")
        }
        redis?.delete("sms:code:$mobile")

        val member = memberLogic.get(userId)
            ?: throw NotFoundException("Member not found: $userId")
        memberLogic.update(member.copy(mobile = mobile))
    }

    @Put("/update-password")
    suspend fun updatePassword(userId: Long, oldPassword: String, newPassword: String) {
        val member = memberLogic.get(userId)
            ?: throw NotFoundException("Member not found: $userId")

        // Verify old password
        if (member.password != null) {
            if (!PasswordEncoder.matches(oldPassword, member.password)) {
                throw BadRequestException("Old password is incorrect")
            }
        }

        // Hash new password and update
        memberLogic.update(member.copy(password = PasswordEncoder.encode(newPassword)))
    }

    @Put("/reset-password")
    suspend fun resetPassword(mobile: String, smsCode: String, newPassword: String) {
        // Verify SMS code
        val storedCode = redis?.getValue("sms:code:$mobile")
        if (storedCode == null) {
            throw BadRequestException("SMS code expired or not sent")
        }
        if (storedCode != smsCode) {
            throw BadRequestException("Invalid SMS code")
        }
        redis?.delete("sms:code:$mobile")

        val member = MemberTable.oneWhere {
            Member::mobile eq mobile
        } ?: throw NotFoundException("Member not found with mobile: $mobile")

        // Hash new password and update
        memberLogic.update(member.copy(password = PasswordEncoder.encode(newPassword)))
    }
}
