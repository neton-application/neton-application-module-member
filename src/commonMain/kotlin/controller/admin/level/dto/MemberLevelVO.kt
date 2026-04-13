package controller.admin.level.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Max
import neton.validation.annotations.Min
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Size

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

@Serializable
data class CreateMemberLevelRequest(
    @property:NotBlank
    @property:Size(min = 1, max = 64)
    val name: String,

    @property:Min(1)
    val level: Int,

    @property:Min(0)
    val experience: Long = 0,

    @property:Min(0)
    @property:Max(100)
    val discount: Int = 100,

    @property:Size(min = 0, max = 255)
    val icon: String? = null,

    @property:Min(0)
    @property:Max(1)
    val status: Int = 1
)

@Serializable
data class UpdateMemberLevelRequest(
    @property:Min(1)
    val id: Long,

    @property:NotBlank
    @property:Size(min = 1, max = 64)
    val name: String,

    @property:Min(1)
    val level: Int,

    @property:Min(0)
    val experience: Long = 0,

    @property:Min(0)
    @property:Max(100)
    val discount: Int = 100,

    @property:Size(min = 0, max = 255)
    val icon: String? = null,

    @property:Min(0)
    @property:Max(1)
    val status: Int = 1
)
