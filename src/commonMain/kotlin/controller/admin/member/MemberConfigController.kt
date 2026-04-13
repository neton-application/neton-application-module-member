package controller.admin.member

import controller.admin.member.dto.MemberPointTradeConfigVO
import controller.admin.member.dto.UpdateMemberPointTradeConfigRequest
import kotlinx.serialization.json.Json
import model.MemberConfig
import table.MemberConfigTable
import neton.database.dsl.*
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put

@Controller("/member/config")
class MemberConfigController {

    @Put("/update")
    suspend fun update(@Body request: UpdateMemberPointTradeConfigRequest) {
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

    @Get("/get")
    suspend fun get(): MemberPointTradeConfigVO {
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

    companion object {
        private const val POINT_TRADE_CONFIG_KEY = "point_trade"
    }
}
