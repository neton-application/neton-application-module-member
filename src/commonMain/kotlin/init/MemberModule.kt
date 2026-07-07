package init

import neton.core.annotations.Module

/** member 模块声明锚点（MANIFEST-P3）。
 *  @Logic: MemberLogic + 8 个 MemberXxxLogic/NicknameGenerator (纯单-Logger);
 *  runtime: init.MemberRuntimeBootstrap (RequiredActions/MemberAuth/
 *  MemberProfile 复杂装配 + 跨模块 nullable 依赖); migrations + 路由由 KSP manifest。 */
// MEMBER-IDENTITY-ADAPTER(M1): 去掉 privchat 依赖 —— member 只依赖 system/infra,
// 身份后端经 MemberIdentityAdapter 抽象;privchat 模式由 module-privchat override adapter。
@Module(dependsOn = ["system", "infra"], migrations = true)
object MemberModule
