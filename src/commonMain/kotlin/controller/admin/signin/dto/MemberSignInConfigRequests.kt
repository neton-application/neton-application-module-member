package controller.admin.signin.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Max
import neton.validation.annotations.Min

@Serializable
data class CreateMemberSignInConfigRequest(
    @property:Min(1)
    @property:Max(365)
    val day: Int,

    @property:Min(0)
    val point: Int,

    @property:Min(0)
    val experience: Int = 0,

    @property:Min(0)
    @property:Max(1)
    val status: Int = 1
)

@Serializable
data class UpdateMemberSignInConfigRequest(
    @property:Min(1)
    val id: Long,

    @property:Min(1)
    @property:Max(365)
    val day: Int,

    @property:Min(0)
    val point: Int,

    @property:Min(0)
    val experience: Int = 0,

    @property:Min(0)
    @property:Max(1)
    val status: Int = 1
)
