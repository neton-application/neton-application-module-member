package controller.app.auth.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Pattern
import neton.validation.annotations.Size

@Serializable
data class MemberLoginRequest(
    @property:NotBlank
    @property:Pattern(regex = "^1\\d{10}$", message = "mobile format is invalid")
    val mobile: String,

    @property:Size(min = 8, max = 128)
    val password: String? = null,

    @property:Size(min = 4, max = 8)
    val smsCode: String? = null
)
