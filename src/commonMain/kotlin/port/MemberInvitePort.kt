package port

/**
 * 邀请自动加好友端口(MEMBER_INVITE_CODE §5.3)。member 不依赖 IM ——
 * 产品装配层(PrivChat)bind 实现,把 inviter/invitee 桥到 privchat-server
 * service API 建双向好友(source='invite_code')。未装配(builtin)= 跳过。
 * 实现必须幂等:已是好友返回成功。
 */
interface MemberInvitePort {
    /** 成功正常返回;失败抛异常(调用方记录 FAILED,不回滚绑定——弱一致拍板 §2-6)。 */
    suspend fun autoFriend(inviterUserId: Long, inviteeUserId: Long, code: String)
}
