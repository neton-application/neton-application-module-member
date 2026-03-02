package controller.admin.member.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserPointUpdateReqVO(
    val id: Long,
    val point: Int
)
