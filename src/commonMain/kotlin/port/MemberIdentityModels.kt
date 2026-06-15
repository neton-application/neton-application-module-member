package port

/**
 * MEMBER-IDENTITY-ADAPTER v1 —— member 账号身份的中立模型(不依赖任何后端)。
 * 见 docs/MEMBER_IDENTITY_MODE_SPEC.md。
 */

/** 账号来源 provider 标识。 */
object MemberIdentityProvider {
    const val BUILTIN = "builtin"
    const val PRIVCHAT = "privchat"
}

/** 注册/登录时产生或绑定底层账号的入参。 */
data class CreateMemberAccountCommand(
    val mobile: String? = null,
    val displayName: String? = null,
    val deviceId: String? = null,
    val platform: String? = null,
)

/**
 * 底层账号引用。
 * - builtin:`externalUserId = null`,member 用自己的本地 id 作主身份;
 * - privchat:`externalUserId = privchat user_id`。
 */
data class MemberAccountRef(
    val provider: String,
    val externalUserId: Long? = null,
    /** 本次是否新建(用于区分注册 vs 命中已有)。 */
    val created: Boolean = false,
)

/** 签发 token 入参。 */
data class IssueMemberTokenCommand(
    val memberId: Long,
    val externalUserId: Long? = null,
    val mobile: String? = null,
    val deviceId: String? = null,
    val platform: String? = null,
)

/** 刷新 token 入参。 */
data class RefreshMemberTokenCommand(
    val refreshToken: String,
    val deviceId: String? = null,
)

/** token 结果(后端无关)。 */
data class MemberTokenBundle(
    val accessToken: String,
    val refreshToken: String? = null,
    val expiresInSeconds: Long? = null,
    val tokenType: String = "Bearer",
)

/** 资料变更事件(作为 adapter 回调入参,定义在 member,不引入独立总线)。 */
data class MemberProfileChangedEvent(val memberId: Long, val changedFields: Set<String>)
data class MemberMobileChangedEvent(val memberId: Long, val newMobile: String, val oldMobile: String? = null)
data class MemberPasswordChangedEvent(val memberId: Long)
