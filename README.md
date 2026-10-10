# neton-application-module-member

会员成长模块。moduleId `member`，依赖 `account`。

负责：会员档案（等级、经验、积分余额、分组）、等级定义与变更记录、积分流水、签到、邀请码与邀请奖励、
会员标签、会员配置（积分抵扣）、收货地址。

账号本身（身份、认证、资料、登录设备与会话）在 `neton-application-module-account`。本模块依赖它，
并通过 `port.AccountExtensions` 接入账号流程（见 `logic/MemberAccountExtensions.kt`）：

- 注册：校验 / 绑定邀请码、派发自动加好友与邀请奖励、发欢迎语
- 登录后强制补全：「填写邀请码」

account 永远不反向依赖本模块。

## 数据

- `member_profiles`：一个账号在会员体系里的状态，惰性创建；余额与经验只做原子更新。
- 等级、积分、签到、邀请、分组、标签、配置、地址：`member_*` 表，表名与拆分前一致。

## 公开接口

App 路由保持拆分前的路径不变：`/member/sign-in`、`/member/invite-code`、`/member/level`、
`/member/point/record`、`/member/experience-record`、`/member/address`；新增 `/member/profile/get`（我的等级、经验、积分）。
后台：等级 / 积分 / 分组的修改在 `/member/user/update-level|update-point|update-group`，权限 `member:profile:update`。

## 迁移

V001–V003 是拆分前的历史脚本（带校验和，不可修改）。V004 把账号表上的成长字段搬到 `member_profiles`，
校验通过后删列；并清理全新安装时 V001 仍会建出的旧名身份表。见文件头注释。
