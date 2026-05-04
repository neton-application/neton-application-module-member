package controller.app.auth.dto

import kotlinx.serialization.Serializable

/**
 * 成员登录响应（spec TOKEN_UNIFICATION_SPEC §8 LoginResponse）。
 *
 * 单一 unified token：
 * - [accessToken] / [refreshToken]：server 签发的 RS256 unified token；HTTP + IM 通用
 * - 客户端拿这一对 token 即可：访问 application 的 app-api 路由组，以及 IM RPC Authenticate 都用 [accessToken]
 *
 * 严禁字段：im_token / im_refresh_token / im_device_id（spec §8.2 终态）
 */
@Serializable
data class MemberLoginResponse(
    val userId: Long,
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val refreshExpiresIn: Long,
    val deviceId: String,
    val sessionVersion: Long,
    val deviceCreated: Boolean,
    val scope: List<String>,
    val issuer: String = "privchat-server",
)
