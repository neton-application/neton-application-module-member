package controller.app.address.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Max
import neton.validation.annotations.Min
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Pattern
import neton.validation.annotations.Size

@Serializable
data class CreateAddressRequest(
    @property:NotBlank
    @property:Size(min = 2, max = 32)
    val name: String,

    @property:NotBlank
    @property:Pattern(regex = "^1\\d{10}$", message = "mobile format is invalid")
    val mobile: String,

    @property:Size(min = 1, max = 32)
    val areaCode: String? = null,

    @property:NotBlank
    @property:Size(min = 5, max = 255)
    val detailAddress: String,

    @property:Min(0)
    @property:Max(1)
    val defaultStatus: Int = 0,
)

@Serializable
data class UpdateAddressRequest(
    @property:Min(1)
    val id: Long,

    @property:NotBlank
    @property:Size(min = 2, max = 32)
    val name: String,

    @property:NotBlank
    @property:Pattern(regex = "^1\\d{10}$", message = "mobile format is invalid")
    val mobile: String,

    @property:Size(min = 1, max = 32)
    val areaCode: String? = null,

    @property:NotBlank
    @property:Size(min = 5, max = 255)
    val detailAddress: String,

    @property:Min(0)
    @property:Max(1)
    val defaultStatus: Int = 0,
)
