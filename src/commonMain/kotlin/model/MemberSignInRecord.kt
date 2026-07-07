package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("member_sign_in_records")
data class MemberSignInRecord(
    @Id
    val id: Long = 0,
    val userId: Long,
    val day: Int,
    val point: Int,
    val experience: Int = 0,
    /** 当次签到发放的现金奖励快照（分；0=未发现金）。对账锚点：ledger biz_id = 本记录 id。 */
    val cashAmount: Long = 0,
    @CreatedAt
    val createdAt: String? = null
)
