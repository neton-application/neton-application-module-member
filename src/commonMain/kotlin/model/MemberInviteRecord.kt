package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Id
import neton.database.annotations.Table

/** 邀请记录(MEMBER_INVITE_CODE §3)。invitee UNIQUE:一个用户只绑定一次邀请来源。 */
@Serializable
@Table("member_invite_records")
data class MemberInviteRecord(
    @Id
    val id: Long = 0,
    val codeId: Long,
    val code: String,
    val inviterUserId: Long? = null,
    val inviteeUserId: Long,
    /** PHONE_SMS / USERNAME_PASSWORD */
    val registerMode: String,
    val registerIdentifierMasked: String? = null,
    /** 1=REGISTER 2=POST_REGISTER_BIND */
    val bindScene: Int,
    /** 0=SKIPPED 1=SUCCESS 2=FAILED */
    val autoFriendStatus: Int = 0,
    val autoFriendError: String? = null,
    val boundAt: Long = 0,
)
