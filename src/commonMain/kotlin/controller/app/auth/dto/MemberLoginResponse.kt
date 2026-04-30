package controller.app.auth.dto

import kotlinx.serialization.Serializable

/**
 * 成员登录响应（spec UPSTREAM §5：privchat 注册/登录响应需多带 im_token / device_id；
 * spec TOKEN_API v1.3：im_refresh_token / im_refresh_expires_in 由 server 返回）。
 *
 * 字段说明：
 * - `userId` / `accessToken` / `refreshToken` / `expiresIn`：application 侧 member token
 *    （access/refresh 的 JWT claim 都绑定 `device_id`，refresh 时校验一致）
 * - `imToken` / `imRefreshToken` / `imDeviceId` / `imExpiresIn` / `imRefreshExpiresIn`
 *    / `sessionVersion` / `deviceCreated`：privchat-server 签发的 IM session
 *    （refresh-member-token 路径不重发 IM，全部为 null —— 客户端走 server 内置 refresh RPC）
 */
@Serializable
data class MemberLoginResponse(
    val userId: Long,
    val accessToken: String,
    val refreshToken: String,
    val expiresIn: Long,
    val imToken: String? = null,
    val imRefreshToken: String? = null,
    val imDeviceId: String? = null,
    val imExpiresIn: Long? = null,
    val imRefreshExpiresIn: Long? = null,
    val sessionVersion: Long? = null,
    val deviceCreated: Boolean? = null,
)
