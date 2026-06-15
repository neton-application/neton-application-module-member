package impl

import port.CreateMemberAccountCommand
import port.IssueMemberTokenCommand
import port.MemberAccountRef
import port.MemberIdentityAdapter
import port.MemberTokenBundle
import port.RefreshMemberTokenCommand

/**
 * 内置身份后端(MEMBER-IDENTITY-ADAPTER v1)。
 *
 * 内置模式 = Neton Application 自带账号体系:member 用自己的本地 id 作主身份,
 * 资料变更无外部同步(继承默认空回调)。
 *
 * **当前(M1):本地 token 签发体系未实现,createOrBind/issue/refresh 明确 unsupported,
 * 绝不静默 fallback 到 privchat**。真正的内置账号 ID 生成 + 本地 token issuer 留后续单独做;
 * 现阶段生产能力由 privchat 模式经 PrivchatMemberIdentityAdapter 提供。
 */
class BuiltinMemberIdentityAdapter : MemberIdentityAdapter {

    override suspend fun createOrBindAccount(command: CreateMemberAccountCommand): MemberAccountRef =
        unsupported("createOrBindAccount")

    override suspend fun issueToken(command: IssueMemberTokenCommand): MemberTokenBundle =
        unsupported("issueToken")

    override suspend fun refreshToken(command: RefreshMemberTokenCommand): MemberTokenBundle =
        unsupported("refreshToken")

    // 资料变更回调:内置无外部同步,继承默认空实现。

    private fun unsupported(op: String): Nothing =
        throw UnsupportedOperationException(
            "MEMBER_IDENTITY_BUILTIN_UNSUPPORTED: builtin 模式的 $op 暂未实现" +
                "(需本地 token issuer / member id 生成器)。当前仅 privchat 模式可用;" +
                "请确认 member.identity.mode 与已安装的 identity adapter。",
        )
}
