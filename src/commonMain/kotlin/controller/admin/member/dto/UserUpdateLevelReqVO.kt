package controller.admin.member.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserUpdateLevelReqVO(
    val id: Long,
    val levelId: Long
)
