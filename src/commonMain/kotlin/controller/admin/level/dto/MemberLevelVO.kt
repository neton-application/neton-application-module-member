package controller.admin.level.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberLevelVO(
    val id: Long = 0,
    val name: String? = null,
    val level: Int? = null,
    val experience: Long? = null,
    val discount: Int? = null,
    val icon: String? = null,
    val status: Int? = null,
    val createdAt: String? = null
)
