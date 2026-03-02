package controller.admin.member.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberGroupVO(
    val id: Long = 0,
    val name: String? = null,
    val remark: String? = null,
    val status: Int? = null,
    val createdAt: String? = null
)
