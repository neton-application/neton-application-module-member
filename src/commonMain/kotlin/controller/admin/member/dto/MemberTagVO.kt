package controller.admin.member.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberTagVO(
    val id: Long = 0,
    val name: String? = null,
    val createdAt: String? = null
)
