package port

/**
 * 注册策略(MEMBER_INVITE_CODE §5.0):产品装配层(PrivChat)从 conf 读取并 bind;
 * 未装配用默认值。bootstrap `/config/bootstrap` 的 auth 块下发同一份给客户端。
 * `inviteCodeRequired` 只作用于**新注册流程**;存量用户登录不受影响(评审 B)。
 */
data class MemberAuthPolicy(
    val registerModes: List<String> = listOf(MODE_PHONE_SMS),
    val defaultRegisterMode: String = MODE_PHONE_SMS,
    val inviteCodeRequired: Boolean = false,
    /**
     * 邀请码强制的注册时间锚(与 member_users.created_at 同格式的字符串,字典序即时间序)。
     * null = 约束所有未绑定用户(默认);配置后只 gate 该时间之后注册的账号——
     * 用于开启 inviteCodeRequired 时豁免存量用户,不必逐个补种子绑定。
     */
    val inviteCodeRequiredSince: String? = null,
    val nicknameRequired: Boolean = false,
) {
    companion object {
        const val MODE_PHONE_SMS = "PHONE_SMS"
        const val MODE_USERNAME_PASSWORD = "USERNAME_PASSWORD"
    }
}
