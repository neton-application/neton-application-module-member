package controller.app.auth.dto

import controller.admin.auth.dto.LoginDeviceInfo
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Size
import kotlinx.serialization.Serializable

/** 账号密码登录(USERNAME_PASSWORD)。 */
@Serializable
data class UsernameLoginRequest(
    @property:NotBlank
    @property:Size(min = 3, max = 32)
    val username: String,

    @property:NotBlank
    @property:Size(min = 8, max = 128)
    val password: String,

    val device: LoginDeviceInfo? = null,
)
