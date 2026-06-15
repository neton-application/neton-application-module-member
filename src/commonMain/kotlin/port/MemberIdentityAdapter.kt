package port

/**
 * member 账号身份后端的抽象(MEMBER-IDENTITY-ADAPTER v1)。
 *
 * member 业务层只依赖本接口,**不直接依赖 PrivchatServiceClient**。
 * - 内置实现 [impl.BuiltinMemberIdentityAdapter](member 自带);
 * - PrivChat 实现 `PrivchatMemberIdentityAdapter`(由 neton-application-module-privchat 提供,Commit 3)。
 *
 * 资料变更回调默认空实现:builtin 不需要外部同步;privchat override 后同步到 privchat-server。
 *
 * 见 docs/MEMBER_IDENTITY_MODE_SPEC.md。
 */
interface MemberIdentityAdapter {

    /** 注册/登录时:产生或绑定底层账号,返回外部账号引用。 */
    suspend fun createOrBindAccount(command: CreateMemberAccountCommand): MemberAccountRef

    /** 签发 token。 */
    suspend fun issueToken(command: IssueMemberTokenCommand): MemberTokenBundle

    /** 刷新 token。 */
    suspend fun refreshToken(command: RefreshMemberTokenCommand): MemberTokenBundle

    /** 资料变更后回调(默认空:builtin 无外部同步)。 */
    suspend fun onProfileChanged(event: MemberProfileChangedEvent) {}
    suspend fun onMobileChanged(event: MemberMobileChangedEvent) {}
    suspend fun onPasswordChanged(event: MemberPasswordChangedEvent) {}
}
