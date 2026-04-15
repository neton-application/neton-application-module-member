package logic

import controller.admin.member.dto.MemberPointTradeConfigVO
import controller.admin.member.dto.UpdateMemberPointTradeConfigRequest
import kotlinx.serialization.json.Json
import model.MemberConfig
import table.MemberConfigTable
import neton.database.dsl.*

import neton.logging.Logger

class MemberConfigLogic(
    private val log: Logger
) {

    companion object {
        private const val POINT_TRADE_CONFIG_KEY = "point_trade"
    }

    suspend fun updatePointTradeConfig(request: UpdateMemberPointTradeConfigRequest) {
        val existing = MemberConfigTable.oneWhere {
            MemberConfig::configKey eq POINT_TRADE_CONFIG_KEY
        }
        val value = Json.encodeToString(MemberPointTradeConfigVO.serializer(), request.toVO())

        if (existing != null) {
            MemberConfigTable.update(existing.copy(value = value))
        } else {
            MemberConfigTable.insert(
                MemberConfig(
                    configKey = POINT_TRADE_CONFIG_KEY,
                    value = value
                )
            )
        }
    }

    suspend fun getPointTradeConfig(): MemberPointTradeConfigVO {
        val config = MemberConfigTable.oneWhere {
            MemberConfig::configKey eq POINT_TRADE_CONFIG_KEY
        } ?: return MemberPointTradeConfigVO()

        return Json.decodeFromString(MemberPointTradeConfigVO.serializer(), config.value)
    }

    private fun UpdateMemberPointTradeConfigRequest.toVO() = MemberPointTradeConfigVO(
        pointTradeDeductEnable = pointTradeDeductEnable,
        pointTradeDeductUnitPrice = pointTradeDeductUnitPrice,
        pointTradeDeductMaxPrice = pointTradeDeductMaxPrice,
        pointTradeGivePoint = pointTradeGivePoint
    )
}
