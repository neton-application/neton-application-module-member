package init

import infra.TableRegistryBuilder
import neton.core.component.NetonContext
import neton.logging.LoggerFactory
import neton.redis.RedisClient
import logic.MessageSendLogic
import logic.SocialUserLogic
import port.MemberIdentityAdapter
import impl.BuiltinMemberIdentityAdapter

import model.*
import table.*
import logic.*

// MANIFEST-P3 + MEMBER-IDENTITY-ADAPTER(M1): 手写 runtime bootstrap。
// member 不再直接依赖 privchat —— 身份后端走 MemberIdentityAdapter:
//   默认 bindIfAbsent(BuiltinMemberIdentityAdapter)(内置,M1 下 auth unsupported);
//   privchat 模式由 module-privchat 的 PrivchatMemberIdentityAdapter override(M2)。
object MemberRuntimeBootstrap {
    fun initialize(ctx: NetonContext) {
        val loggerFactory = ctx.get(LoggerFactory::class)
        val registry = ctx.get(TableRegistryBuilder::class)
        val appFileLogic = ctx.get(logic.FileUploadLogic::class)

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

        // 身份后端 adapter:默认内置;privchat 模式下 module-privchat 已 bind override → bindIfAbsent 保留它。
        ctx.bindIfAbsent(MemberIdentityAdapter::class, BuiltinMemberIdentityAdapter())
        val identityAdapter = ctx.get(MemberIdentityAdapter::class)

        // 跨模块依赖（nullable: 对应模块未装配时降级）
        val redis = ctx.getOrNull(RedisClient::class)
        val messageSendLogic = ctx.getOrNull(MessageSendLogic::class)
        val socialUserLogic = ctx.getOrNull(SocialUserLogic::class)

        // R8.4a 顺序: MemberLogic (@Logic 已 bind) → RequiredActionsLogic → MemberAuthLogic。
        val memberLogic = ctx.get(MemberLogic::class)
        val requiredActionsLogic = RequiredActionsLogic(memberLogic)
        ctx.bind(RequiredActionsLogic::class, requiredActionsLogic)
        ctx.bind(MemberAuthLogic::class, MemberAuthLogic(
            log = loggerFactory.get("logic.member-auth"),
            identityAdapter = identityAdapter,
            requiredActionsLogic = requiredActionsLogic,
            redis = redis,
            messageSendLogic = messageSendLogic,
            socialUserLogic = socialUserLogic
        ))
        ctx.bind(MemberProfileLogic::class, MemberProfileLogic(
            log = loggerFactory.get("logic.member-profile"),
            memberLogic = memberLogic,
            appFileLogic = appFileLogic,
            identityAdapter = identityAdapter,
        ))

        // 签到：现金奖励发放端口（可空）。产品装配层（privchat @Module dependsOn=[] 先 init）
        // bind MemberRewardPort 后签到可发现金；未装配（builtin）= null，纯积分行为。
        ctx.bind(MemberSignInLogic::class, MemberSignInLogic(
            log = loggerFactory.get("logic.member-signin"),
            db = ctx.get(neton.database.api.DbContext::class),
            rewardPort = ctx.getOrNull(port.MemberRewardPort::class),
        ))
    }
}
