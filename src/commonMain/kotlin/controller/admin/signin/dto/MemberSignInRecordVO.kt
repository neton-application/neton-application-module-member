package controller.admin.signin.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberSignInRecordVO(
    val id: Long = 0,
    val userId: Long? = null,
    /** 取自 member_users；签到记录表本身只存 userId。用户已删时为 null。 */
    val nickname: String? = null,
    val day: Int? = null,
    val point: Int? = null,
    val experience: Int? = null,
    /** 当次发放的现金奖励快照（分；0=未发现金）。 */
    val cashAmount: Long = 0,
    val createdAt: Long? = null
)
