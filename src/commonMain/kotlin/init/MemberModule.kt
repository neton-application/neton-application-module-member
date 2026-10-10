package init

import neton.core.annotations.Module

/**
 * member 模块声明锚点：会员成长。
 *
 * 等级、经验、积分、签到、邀请、分组、标签、会员配置、收货地址。账号本身（身份、认证、资料、
 * 设备与会话）在 account 模块；本模块依赖它、并通过 [port.AccountExtensions] 接入注册与
 * 登录后补全流程，account 永远不反向依赖本模块。
 *
 * runtime 装配见 [MemberRuntimeBootstrap]。
 */
@Module(dependsOn = ["system", "infra", "account"], migrations = true)
object MemberModule
