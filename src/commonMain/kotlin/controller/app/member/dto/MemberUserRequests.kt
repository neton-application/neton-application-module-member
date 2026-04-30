package controller.app.member.dto

import controller.app.auth.dto.E164_MESSAGE
import controller.app.auth.dto.E164_REGEX
import kotlinx.serialization.Serializable
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Pattern
import neton.validation.annotations.Size

@Serializable
data class UpdateMemberProfileRequest(
    @property:NotBlank
    @property:Size(min = 2, max = 32)
    val nickname: String,

    @property:Size(min = 1, max = 512)
    val avatar: String? = null,
)

@Serializable
data class UpdateMemberMobileRequest(
    @property:NotBlank
    @property:Pattern(regex = E164_REGEX, message = E164_MESSAGE)
    val mobile: String,

    @property:NotBlank
    @property:Size(min = 4, max = 8)
    val smsCode: String,
)

@Serializable
data class UpdateMemberPasswordRequest(
    @property:NotBlank
    @property:Size(min = 8, max = 128)
    val oldPassword: String,

    @property:NotBlank
    @property:Size(min = 8, max = 128)
    val newPassword: String,
)

@Serializable
data class ResetMemberPasswordRequest(
    @property:NotBlank
    @property:Pattern(regex = E164_REGEX, message = E164_MESSAGE)
    val mobile: String,

    @property:NotBlank
    @property:Size(min = 4, max = 8)
    val smsCode: String,

    @property:NotBlank
    @property:Size(min = 8, max = 128)
    val newPassword: String,
)
