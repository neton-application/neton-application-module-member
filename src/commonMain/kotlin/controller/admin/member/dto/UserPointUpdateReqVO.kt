package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Min

@Serializable
data class UpdateMemberUserPointRequest(
    @property:Min(1)
    val id: Long,

    val point: Int
)
