package init

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.hook.HookBus
import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.logging.LoggerFactory
import neton.redis.RedisClient
import logic.MessageSendLogic
import logic.SocialUserLogic

import model.*
import table.*
import logic.*

// MANIFEST-P3: 手写 runtime bootstrap。MemberLogic + 8 个纯 logic 已标 @Logic →
// 生成的 MemberLogicInitializer 装配 (manifest 顺序: logics → 本 bootstrap → routes,
// 所以这里 ctx.get(MemberLogic) 拿得到)。moduleId/dependsOn/migrations/路由 由 manifest。
// 这里留: Table 注册 + 带 nullable 跨模块依赖 / inter-logic 依赖的 3 个复杂装配。
object MemberRuntimeBootstrap {
    fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val registry = ctx.get(TableRegistryBuilder::class)
        val privchatService = ctx.get(PrivchatServiceClient::class)
        val appFileLogic = ctx.get(logic.AppFileLogic::class)

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
        registry.register(MemberNicknameAdjective::class, MemberNicknameAdjectiveTable)
        registry.register(MemberNicknameNoun::class, MemberNicknameNounTable)

        // 跨模块依赖（nullable: 对应模块未装配时降级）
        val redis = ctx.getOrNull(RedisClient::class)
        val messageSendLogic = ctx.getOrNull(MessageSendLogic::class)
        val socialUserLogic = ctx.getOrNull(SocialUserLogic::class)
        val hookBus = ctx.getOrNull(HookBus::class)

        // R8.4a 顺序: MemberLogic (@Logic 已 bind) → RequiredActionsLogic → MemberAuthLogic。
        val memberLogic = ctx.get(MemberLogic::class)
        val requiredActionsLogic = RequiredActionsLogic(memberLogic)
        ctx.bind(RequiredActionsLogic::class, requiredActionsLogic)
        ctx.bind(MemberAuthLogic::class, MemberAuthLogic(
            log = loggerFactory.get("logic.member-auth"),
            privchatService = privchatService,
            requiredActionsLogic = requiredActionsLogic,
            redis = redis,
            messageSendLogic = messageSendLogic,
            socialUserLogic = socialUserLogic
        ))
        ctx.bind(MemberProfileLogic::class, MemberProfileLogic(
            log = loggerFactory.get("logic.member-profile"),
            memberLogic = memberLogic,
            appFileLogic = appFileLogic,
            hookBus = hookBus,
        ))
    }
}
