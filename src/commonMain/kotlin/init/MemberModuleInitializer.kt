package init

import com.netonstream.privchat.application.module.privchat.client.PrivchatServiceClient
import com.netonstream.privchat.application.module.privchat.hook.HookBus
import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.core.module.MigrationDialect
import neton.core.module.MigrationSource
import neton.core.module.ModuleInitializer
import neton.logging.LoggerFactory
import neton.redis.RedisClient
import logic.MessageSendLogic
import logic.SocialUserLogic

import model.*
import table.*
import logic.*

object MemberModuleInitializer : ModuleInitializer {

    override val moduleId: String = "member"
    override val dependsOn: List<String> = listOf("system", "privchat", "infra")

    /** 部署后路径,由 application/build.gradle.kts `copyModuleMigrations` task 填充. */
    override fun migrations(): List<MigrationSource> = listOf(
        MigrationSource(
            moduleId = moduleId,
            dialect = MigrationDialect.POSTGRESQL,
            resourcePath = "migrations/$moduleId/postgresql",
        )
    )

    override fun initialize(ctx: NetonContext) {
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

        // 从 system 模块获取 Provider Logic（跨模块依赖）
        val redis = ctx.getOrNull(RedisClient::class)
        val messageSendLogic = ctx.getOrNull(MessageSendLogic::class)
        val socialUserLogic = ctx.getOrNull(SocialUserLogic::class)
        val hookBus = ctx.getOrNull(HookBus::class)

        // 绑定 Logic
        // R8.4a 顺序约束：MemberLogic / RequiredActionsLogic 必须先 bind，
        // 因为 MemberAuthLogic 依赖 RequiredActionsLogic（计算 Post-login
        // Required Actions 注入到 LoginResponse），RequiredActionsLogic
        // 依赖 MemberLogic（从 uid 取 member 算 nickname pattern）。
        val memberLogic = MemberLogic(loggerFactory.get("logic.member"))
        ctx.bind(MemberLogic::class, memberLogic)
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
        ctx.bind(MemberLevelLogic::class, MemberLevelLogic(loggerFactory.get("logic.member-level")))
        ctx.bind(MemberPointLogic::class, MemberPointLogic(loggerFactory.get("logic.member-point")))
        ctx.bind(MemberSignInLogic::class, MemberSignInLogic(loggerFactory.get("logic.member-signin")))
        ctx.bind(MemberGroupLogic::class, MemberGroupLogic(loggerFactory.get("logic.member-group")))
        ctx.bind(MemberTagLogic::class, MemberTagLogic(loggerFactory.get("logic.member-tag")))
        ctx.bind(MemberAddressLogic::class, MemberAddressLogic(loggerFactory.get("logic.member-address")))
        ctx.bind(MemberConfigLogic::class, MemberConfigLogic(loggerFactory.get("logic.member-config")))

        // 注册 KSP 生成的路由
        neton.module.member.generated.MemberRouteInitializer.initialize(ctx)
    }
}
