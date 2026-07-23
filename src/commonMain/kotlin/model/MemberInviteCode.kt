package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Id
import neton.database.annotations.Table

/** 邀请码(MEMBER_INVITE_CODE §3)。owner 为空 = 平台码(只记录来源,不自动加好友)。 */
@Serializable
@Table("member_invite_codes")
data class MemberInviteCode(
    @Id
    val id: Long = 0,
    val code: String,
    val ownerUserId: Long? = null,
    /** 0=不限次 */
    val maxUses: Int = 0,
    val usedCount: Int = 0,
    /** 1=启用 0=停用 */
    val status: Int = 1,
    /** ms;null=不过期 */
    val expiresAt: Long? = null,
    val remark: String? = null,
    /** 绑码成功后邀请人自动发送的打招呼用语;空=用全局 conf invite_welcome_message 兜底。 */
    val welcomeMessage: String? = null,
    val createdBy: Long = 0,
    val createdAt: Long = 0,
    val updatedAt: Long = 0,
)
