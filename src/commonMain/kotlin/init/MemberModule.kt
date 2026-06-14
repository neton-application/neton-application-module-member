package init

import neton.core.annotations.Module

/** member 模块声明锚点（MANIFEST-P3）。
 *  @Logic: MemberLogic + 8 个 MemberXxxLogic/NicknameGenerator (纯单-Logger);
 *  runtime: init.MemberRuntimeBootstrap (Table 注册 + RequiredActions/MemberAuth/
 *  MemberProfile 复杂装配 + 跨模块 nullable 依赖); migrations + 路由由 KSP manifest。 */
@Module(dependsOn = ["system", "privchat", "infra"])
object MemberModule
