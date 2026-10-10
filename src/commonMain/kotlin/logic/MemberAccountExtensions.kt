package logic

import controller.app.auth.dto.RequiredAction
import model.Account
import neton.logging.Logger
import port.AccountAuthPolicy
import port.AccountRegistrationContext
import port.AccountRegistrationHook
import port.RequiredActionAnchors
import port.RequiredActionContributor

/**
 * 邀请能力接入账号注册流程。
 *
 * 拆分前这段逻辑写死在账号的注册代码里；现在账号只提供扩展点、不认识邀请码，由这里接管。
 * 时序与拆分前一致：
 *
 *   validate：注册请求带了邀请码就悲观校验，无效 = 整次注册失败，账号不会建出来
 *   afterRegistered：绑定邀请记录 → 派发自动加好友 → 派发邀请奖励 → 发欢迎语
 *
 * 欢迎语对**所有**新注册账号都发（不论是否带码），失败只记日志，不影响注册 —— 与拆分前一致。
 */
class MemberInviteRegistrationHook(
    private val inviteLogic: MemberInviteLogic,
    private val welcomePort: port.MemberWelcomePort?,
    private val log: Logger,
) : AccountRegistrationHook {

    override val consumesInviteCode: Boolean = true

    override suspend fun validate(context: AccountRegistrationContext) {
        context.inviteCode?.let { inviteLogic.validate(it) }
    }

    override suspend fun afterRegistered(context: AccountRegistrationContext, account: Account) {
        context.inviteCode?.let { code ->
            // 再取一次码实体：钩子实例在并发注册之间共享，不能把 validate 的结果存在字段里。
            // applyInviteForNewUser 在同一事务里复核次数与有效期，校验与落库之间的竞争由它兜住。
            val entity = inviteLogic.validate(code)
            val record = inviteLogic.applyInviteForNewUser(entity, account.id, context.mode, context.identifierMasked)
            inviteLogic.dispatchAutoFriend(record.id)
            inviteLogic.dispatchInviteReward(record.id)
        }
        welcomePort?.let { port ->
            runCatching { port.sendWelcome(account.id) }
                .onFailure {
                    log.warn("member.register.welcome_failed", mapOf("userId" to account.id, "error" to (it.message ?: "")))
                }
        }
    }
}

/**
 * 登录后强制补全「填写邀请码」。
 *
 * 开关仍在账号的注册策略里（[AccountAuthPolicy.inviteCodeRequired]）：那是产品装配层从配置读入、
 * 并经 bootstrap 下发给客户端的注册策略，留在原处，privchat 的配置与 bootstrap 都不用改。
 * 账号只认识这个开关，不认识邀请码；判断与下发由这里负责。
 *
 * 排在「绑定手机号」之后、「设置昵称」之前：前两步都是「这个账号属于谁」，昵称是展示层。
 */
class MemberInviteRequiredAction(
    private val inviteLogic: MemberInviteLogic,
    private val authPolicy: AccountAuthPolicy,
) : RequiredActionContributor {

    override val order: Int = 200

    override suspend fun contribute(account: Account): List<RequiredAction> {
        if (!authPolicy.inviteCodeRequired) return emptyList()
        // 已绑定的用户（含存量）永不触发；配了时间锚则只约束锚点之后注册的账号。
        if (!RequiredActionAnchors.isAnchorHit(account, authPolicy.inviteCodeRequiredSince)) return emptyList()
        if (inviteLogic.myBinding(account.id) != null) return emptyList()
        return listOf(
            RequiredAction(
                action = "bind_invite_code",
                required = true,
                title = "填写邀请码",
                titleKey = "requiredAction.bindInviteCode",
                fields = listOf("inviteCode"),
            ),
        )
    }
}
