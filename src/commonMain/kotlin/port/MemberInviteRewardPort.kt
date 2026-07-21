package port

/**
 * 邀请达成事件。member 只上报事实，不含任何奖励语义。
 *
 * @param totalInvited 邀请人累计成功邀请数（含本次）；奖励规则的阈值锚点。
 * @param recordId     邀请记录 id；下游发奖的幂等锚点。
 */
data class MemberInviteRewardEvent(
    val inviterUserId: Long,
    val inviteeUserId: Long,
    val recordId: Long,
    val code: String,
    val totalInvited: Long,
)

/**
 * 邀请奖励回调（统一封装的扩展点，与 [MemberInvitePort]/[MemberRewardPort] 同款分层）。
 *
 * member 不依赖任何业务/资金模块，也不知道有哪些奖励；邀请达成后由
 * [logic.MemberInviteLogic.dispatchInviteReward] 遍历所有已注册回调触发。
 * 各业务模块（content 发观影券、将来别的模块开 VIP…）实现本接口，在 application
 * 装配层注册进 [MemberInviteRewardRegistry]。加新奖励零改 member。
 *
 * 语义（弱一致）：
 * - 在邀请绑定事务**提交后**触发（与 autoFriend 同位），奖励失败不回滚注册。
 * - 单个回调抛异常只记录、不影响其它回调（各自幂等）。
 */
interface MemberInviteRewardListener {
    suspend fun onInviteReward(event: MemberInviteRewardEvent)
}

/**
 * 邀请奖励回调注册表。application 装配层收集所有业务模块的回调构造后 bind；
 * member 邀请达成时遍历触发。未装配 = 无奖励（纯邀请，行为不变）。
 */
class MemberInviteRewardRegistry(val listeners: List<MemberInviteRewardListener>)
