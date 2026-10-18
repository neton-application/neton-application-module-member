package logic

import neton.core.http.ValidationError

/**
 * member 模块的业务错误机器码（spec application/member-errors）。
 *
 * 规则：异常的 `message` 只放这里的机器码（`^[A-Z][A-Z0-9_]{2,}$`），前端按码翻译
 * （模块语言包 `error.<CODE>`）。数字 `code` 仍取 `NetonErrorCode` 的类别。跟请求字段有关的
 * 细节放字段级错误 [field]，不拼进 `message`；只对排障有用的细节写日志，不进响应。
 *
 * 新增码：先加在这里，再补 spec 与 neton-application-front-member 两种语言的 `error.<CODE>`。
 */
object MemberErrorCodes {
    // ---- 会员标签 / 分组 ----
    const val MEMBER_TAG_NOT_FOUND = "MEMBER_TAG_NOT_FOUND"
    const val MEMBER_GROUP_NOT_FOUND = "MEMBER_GROUP_NOT_FOUND"

    // ---- 收货地址 ----
    /** 地址不存在或不属于当前用户（不区分，避免探测他人地址）。 */
    const val ADDRESS_NOT_FOUND = "ADDRESS_NOT_FOUND"
    /** 收货人手机号格式不对（字段错误 `mobile`，来自 `@Pattern`）。 */
    const val ADDRESS_MOBILE_INVALID = "ADDRESS_MOBILE_INVALID"

    // ---- 邀请码 ----
    /** 邀请码不可用：不存在 / 停用 / 过期 / 用尽 / 邀请人停用统一成这一个码，防枚举。 */
    const val INVITE_CODE_INVALID = "INVITE_CODE_INVALID"
    /** 当前用户已绑定过邀请人。 */
    const val INVITE_ALREADY_BOUND = "INVITE_ALREADY_BOUND"
    /** 随机生成邀请码连续撞码，稍后重试。 */
    const val INVITE_CODE_GENERATION_CONFLICT = "INVITE_CODE_GENERATION_CONFLICT"
    /** 自定义邀请码须为 1-32 位非空白字符（字段 `code`）。 */
    const val INVITE_CODE_FORMAT_INVALID = "INVITE_CODE_FORMAT_INVALID"
    /** 自定义邀请码已被占用（字段 `code`）。 */
    const val INVITE_CODE_ALREADY_EXISTS = "INVITE_CODE_ALREADY_EXISTS"
    /** 后台按 id 操作的邀请码不存在。 */
    const val INVITE_CODE_NOT_FOUND = "INVITE_CODE_NOT_FOUND"
    const val INVITE_RECORD_NOT_FOUND = "INVITE_RECORD_NOT_FOUND"
    /** 后台补偿自动加好友仍失败；具体原因见邀请记录的 autoFriendError。 */
    const val INVITE_AUTO_FRIEND_RETRY_FAILED = "INVITE_AUTO_FRIEND_RETRY_FAILED"

    // ---- 签到 ----
    /** 签到设置入口只能改 `member.signin.*`（字段 `key`）。 */
    const val SIGN_IN_SETTING_KEY_FORBIDDEN = "SIGN_IN_SETTING_KEY_FORBIDDEN"
    /** 宿主未装配全局设置，签到设置只读。 */
    const val SIGN_IN_SETTINGS_READ_ONLY = "SIGN_IN_SETTINGS_READ_ONLY"
    /** 签到天数超出当前连续签到周期（字段 `day`）。 */
    const val SIGN_IN_DAY_OUT_OF_RANGE = "SIGN_IN_DAY_OUT_OF_RANGE"
    /** 该天已有签到配置（字段 `day`）。 */
    const val SIGN_IN_CONFIG_DAY_EXISTS = "SIGN_IN_CONFIG_DAY_EXISTS"
    const val SIGN_IN_CONFIG_NOT_FOUND = "SIGN_IN_CONFIG_NOT_FOUND"
    const val ALREADY_SIGNED_IN_TODAY = "ALREADY_SIGNED_IN_TODAY"
    /** 配了现金奖励但宿主未装配发放端口：拒绝签到，不允许现金静默不发。 */
    const val SIGN_IN_CASH_REWARD_UNAVAILABLE = "SIGN_IN_CASH_REWARD_UNAVAILABLE"

    /** 单字段错误：message 与 code 同为机器码。 */
    fun field(path: String, code: String): List<ValidationError> = listOf(ValidationError(path, code, code))
}
