package controller.app.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberLoginRequest(
    val mobile: String,
    val password: String? = null,
    val smsCode: String? = null
)
