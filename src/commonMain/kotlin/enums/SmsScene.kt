package enums

/**
 * 会员短信验证码发送场景枚举
 *
 * scene 整数与前端约定，templateCode 对应短信模板在系统中的编码（由 MessageSendLogic 查表匹配）。
 */
enum class SmsScene(
    val scene: Int,
    val templateCode: String,
    val description: String
) {
    MEMBER_LOGIN(1, "member_login", "会员用户 - 手机号登录"),
    MEMBER_UPDATE_MOBILE(2, "member_update_mobile", "会员用户 - 修改手机"),
    MEMBER_UPDATE_PASSWORD(3, "member_update_password", "会员用户 - 修改密码"),
    MEMBER_RESET_PASSWORD(4, "member_reset_password", "会员用户 - 忘记密码");

    companion object {
        fun fromScene(scene: Int): SmsScene =
            entries.firstOrNull { it.scene == scene }
                ?: throw IllegalArgumentException("Unknown SMS scene: $scene")
    }
}
