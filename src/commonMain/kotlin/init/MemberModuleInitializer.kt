package init

import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.core.module.ModuleInitializer
import neton.logging.LoggerFactory
import logic.MessageSendLogic
import logic.SocialUserLogic

import model.*
import table.*
import logic.*

object MemberModuleInitializer : ModuleInitializer {

    override val moduleId: String = "member"
    override val dependsOn: List<String> = listOf("system")

    override fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val registry = ctx.get(TableRegistryBuilder::class)

        // 注册 Table
        registry.register(Member::class, MemberTable)
        registry.register(MemberLevel::class, MemberLevelTable)
        registry.register(MemberLevelRecord::class, MemberLevelRecordTable)
        registry.register(MemberPointRecord::class, MemberPointRecordTable)
        registry.register(MemberSignInConfig::class, MemberSignInConfigTable)
        registry.register(MemberSignInRecord::class, MemberSignInRecordTable)
        registry.register(MemberGroup::class, MemberGroupTable)
        registry.register(MemberTag::class, MemberTagTable)
        registry.register(MemberConfig::class, MemberConfigTable)
        registry.register(Address::class, AddressTable)

        // 从 system 模块获取 Provider Logic（跨模块依赖）
        val messageSendLogic = ctx.getOrNull(MessageSendLogic::class)
        val socialUserLogic = ctx.getOrNull(SocialUserLogic::class)

        // 绑定 Logic
        ctx.bind(MemberAuthLogic::class, MemberAuthLogic(
            log = loggerFactory.get("logic.member-auth"),
            messageSendLogic = messageSendLogic,
            socialUserLogic = socialUserLogic
        ))
        ctx.bind(MemberLogic::class, MemberLogic(loggerFactory.get("logic.member")))
        ctx.bind(MemberLevelLogic::class, MemberLevelLogic(loggerFactory.get("logic.member-level")))
        ctx.bind(MemberPointLogic::class, MemberPointLogic(loggerFactory.get("logic.member-point")))
        ctx.bind(MemberSignInLogic::class, MemberSignInLogic(loggerFactory.get("logic.member-signin")))

        // 注册 KSP 生成的路由
        neton.module.member.generated.MemberRouteInitializer.initialize(ctx)
    }
}
