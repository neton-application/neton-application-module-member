package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_sign_in_configs")
data class MemberSignInConfig(
    @Id
    val id: Long = 0,
    val day: Int,
    val point: Int,
    val experience: Int = 0,
    /**
     * 该天签到额外现金奖励（单位：分）。0 = 纯积分/经验（存量行为）；
     * > 0 = 通过 [port.MemberRewardPort] 发放现金（产品侧通常入钱包）。
     */
    val cashAmount: Long = 0,
    val status: Int = 1,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
