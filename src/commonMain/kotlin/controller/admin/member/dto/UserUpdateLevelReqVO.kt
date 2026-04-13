package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Min

@Serializable
data class UpdateMemberUserLevelRequest(
    @property:Min(1)
    val id: Long,

    @property:Min(0)
    val levelId: Long
)
