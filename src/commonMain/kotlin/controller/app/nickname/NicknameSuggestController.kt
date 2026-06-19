package controller.app.nickname

import kotlinx.serialization.Serializable
import logic.NicknameGenerator
import neton.core.annotations.AllowAnonymous
import neton.core.annotations.Controller
import neton.core.annotations.Get

/**
 * 自动昵称建议 —— 给客户端「填昵称引导页」prefill 用。
 *
 * 设计:
 *   - `@AllowAnonymous` 允许未登录也调用 (注册流程进引导页时 token 已颁发,但
 *     有些客户端在 `RequiredAction` 阶段不一定带新 token,放宽避免链路卡)
 *   - 词库未初始化(任一边空)时返回 `suggestion = null`,客户端走旧默认
 *     (`Member_<手机后4位>`) 兜底
 *   - 客户端可多次调获取新建议(用户点「换一个」)
 */
@Controller("/member/nickname-suggest")
class NicknameSuggestController(
    private val nicknameGenerator: NicknameGenerator,
) {

    @Get("/")
    @AllowAnonymous
    suspend fun suggest(): NicknameSuggestResponse {
        val s = nicknameGenerator.next()
        return NicknameSuggestResponse(suggestion = s)
    }
}

/**
 * Wire DTO:
 * ```json
 * { "suggestion": "勇敢的狮子8421" }   // 词库 OK
 * { "suggestion": null }                // 词库空,客户端走兜底
 * ```
 */
@Serializable
data class NicknameSuggestResponse(
    val suggestion: String?,
)
