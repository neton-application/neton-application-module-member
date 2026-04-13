package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Max
import neton.validation.annotations.Min

@Serializable
data class MemberPointTradeConfigVO(
    val pointTradeDeductEnable: Int = 0,
    val pointTradeDeductUnitPrice: Int = 0,
    val pointTradeDeductMaxPrice: Int = 0,
    val pointTradeGivePoint: Int = 0
)

@Serializable
data class UpdateMemberPointTradeConfigRequest(
    @property:Min(0)
    @property:Max(1)
    val pointTradeDeductEnable: Int,

    @property:Min(0)
    val pointTradeDeductUnitPrice: Int,

    @property:Min(0)
    val pointTradeDeductMaxPrice: Int,

    @property:Min(0)
    val pointTradeGivePoint: Int
)
