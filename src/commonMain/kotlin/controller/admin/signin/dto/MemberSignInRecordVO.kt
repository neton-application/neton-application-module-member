package controller.admin.signin.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberSignInRecordVO(
    val id: Long = 0,
    val userId: Long? = null,
    val nickname: String? = null,
    val day: Int? = null,
    val point: Int? = null,
    val experience: Int? = null,
    val createdAt: String? = null
)
