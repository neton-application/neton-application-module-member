package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Min
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Size

@Serializable
data class MemberTagVO(
    val id: Long = 0,
    val name: String? = null,
    val createdAt: String? = null
)

@Serializable
data class CreateMemberTagRequest(
    @property:NotBlank
    @property:Size(min = 1, max = 64)
    val name: String
)

@Serializable
data class UpdateMemberTagRequest(
    @property:Min(1)
    val id: Long,

    @property:NotBlank
    @property:Size(min = 1, max = 64)
    val name: String
)
