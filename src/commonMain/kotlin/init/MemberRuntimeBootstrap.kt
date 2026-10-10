package init

import neton.core.component.NetonContext
import neton.logging.LoggerFactory

import logic.*

/**
 * member（会员成长）手写 runtime 装配。
 *
 * member 依赖 account，拓扑序在其后，所以这里能拿到 account bind 的 [port.AccountExtensions]，
 * 把邀请能力登记进去。账号模块本身不认识邀请码。
 */
object MemberRuntimeBootstrap {
    fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val db = ctx.get(neton.database.api.DbContext::class)
        val memberLogic = ctx.get(MemberLogic::class)

        // 邀请码：自动加好友端口、邀请奖励由产品装配层 bind（privchat），未装配 = 仅记录来源。
        val inviteLogic = MemberInviteLogic(
            log = loggerFactory.get("logic.member-invite"),
            db = db,
            invitePort = ctx.getOrNull(port.MemberInvitePort::class),
            rewardRegistry = ctx.getOrNull(port.MemberInviteRewardRegistry::class),
        )
        ctx.bind(MemberInviteLogic::class, inviteLogic)

        // 接入账号流程：注册时校验 / 绑定邀请码、发欢迎语；登录后强制补全「填写邀请码」。
        // 「邀请码必填」的开关留在账号的注册策略里（产品装配层从 conf 读、bootstrap 下发给客户端）。
        val authPolicy = ctx.getOrNull(port.AccountAuthPolicy::class) ?: port.AccountAuthPolicy()
        val extensions = ctx.get(port.AccountExtensions::class)
        extensions.addRegistrationHook(
            MemberInviteRegistrationHook(
                inviteLogic = inviteLogic,
                welcomePort = ctx.getOrNull(port.MemberWelcomePort::class),
                log = loggerFactory.get("logic.member-invite"),
            ),
        )
        extensions.addRequiredActionContributor(MemberInviteRequiredAction(inviteLogic, authPolicy))

        // 把本模块的配置定义登记进注册表。各模块各自登记，装配层不必知道谁有哪些配置。
        ctx.getOrNull(setting.SettingDefinitionRegistry::class)?.register(setting.MemberSettingKeys.definitions)

        // 签到：现金奖励发放端口可空。产品装配层 bind MemberRewardPort 后签到可发现金；
        // 未装配 = 纯积分 / 经验。积分与经验写在会员档案上（member_profiles）。
        ctx.bind(MemberSignInLogic::class, MemberSignInLogic(
            log = loggerFactory.get("logic.member-signin"),
            db = db,
            memberLogic = memberLogic,
            rewardPort = ctx.getOrNull(port.MemberRewardPort::class),
            // 全局设置读取（连续签到周期）。未装配 = 用代码默认值，签到照常工作，
            // 只是不能在后台调周期。
            configs = ctx.getOrNull(logic.SystemSettingLogic::class),
        ))
    }
}
