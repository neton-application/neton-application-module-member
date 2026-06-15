package impl

import port.CreateMemberAccountCommand
import port.IssueMemberTokenCommand
import port.MemberAccountRef
import port.MemberIdentityAdapter
import port.MemberIdentityProvider
import port.MemberTokenBundle
import port.RefreshMemberTokenCommand

/**
 * 内置身份后端(MEMBER-IDENTITY-ADAPTER v1)。
 *
 * 内置模式 = Neton Application 自带账号体系:member 用自己的本地 id 作主身份,
 * 不创建外部账号、不绑定 PrivChat、资料变更无外部同步(继承默认空回调)。
 *
 * 注意:`issueToken` / `refreshToken` 的本地签发在 **Commit 2** 接入 member/application
 * 现有 token 机制(本刀不新造 token 体系、不改业务流,故此处先留待接线)。
 */
class BuiltinMemberIdentityAdapter : MemberIdentityAdapter {

    override suspend fun createOrBindAccount(command: CreateMemberAccountCommand): MemberAccountRef =
        // 内置:不创建外部账号,member 本地 id 即主身份。
        MemberAccountRef(provider = MemberIdentityProvider.BUILTIN, externalUserId = null, created = false)

    override suspend fun issueToken(command: IssueMemberTokenCommand): MemberTokenBundle =
        TODO("Commit 2: 接入 member/application 本地 token 签发(沿用现有机制,不新造 token 体系)")

    override suspend fun refreshToken(command: RefreshMemberTokenCommand): MemberTokenBundle =
        TODO("Commit 2: 接入本地 refresh token")

    // onProfileChanged / onMobileChanged / onPasswordChanged:继承默认空实现(内置无外部同步)。
}
