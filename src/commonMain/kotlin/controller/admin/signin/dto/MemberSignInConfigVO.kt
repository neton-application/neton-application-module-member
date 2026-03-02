package controller.admin.signin.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberSignInConfigVO(
    val id: Long = 0,
    val day: Int? = null,
    val point: Int? = null,
    val experience: Int? = null,
    val status: Int? = null,
    val createdAt: String? = null
)
