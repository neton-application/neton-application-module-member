package controller.app.auth.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberLoginResponse(
    val userId: Long,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long
)
