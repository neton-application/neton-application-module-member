package port

/**
 * 签到现金奖励事件（spec privchat-docs `07-application/MEMBER_SIGN_IN_REWARD_SPEC`）。
 *
 * @param cashAmount 单位分，> 0 才会触发 [MemberRewardPort.onSignInCashReward]。
 * @param recordId 签到记录 id；下游入账的幂等锚点（ledger biz_id）。
 */
data class SignInCashRewardEvent(
    val userId: Long,
    val recordId: Long,
    val day: Int,
    val cashAmount: Long,
)

/**
 * member 现金/权益奖励发放的产品端口（与 [MemberIdentityAdapter] 同款 port/adapter 分层）。
 *
 * member 是通用模块，不依赖任何资金/产品模块；现金奖励的实际发放
 * （如 PrivChat 入钱包）由产品装配层提供实现（`ctx.bind(MemberRewardPort::class, ...)`）。
 *
 * 语义（fail-fast，资金一致性）：
 * - 在签到 DB 事务内被调用；实现方的 DB 副作用加入同一事务。
 * - 实现方抛异常 = 签到整体失败回滚（不出现「签了到没到账」）。
 * - **配置了现金奖励但产品未装配本 port 时，签到必须失败**（防止静默不发），
 *   由调用方（[logic.MemberSignInLogic]）负责该判定。
 */
interface MemberRewardPort {

    /** 发放签到现金奖励；幂等由实现方按 [SignInCashRewardEvent.recordId] 保证。 */
    suspend fun onSignInCashReward(event: SignInCashRewardEvent)
}
