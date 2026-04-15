package controller.admin.member

import controller.admin.member.dto.MemberPointTradeConfigVO
import controller.admin.member.dto.UpdateMemberPointTradeConfigRequest
import logic.MemberConfigLogic
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Put

@Controller("/member/config")
class MemberConfigController(
    private val configLogic: MemberConfigLogic
) {

    @Put("/update")
    @Permission("member:config:update")
    suspend fun update(@Body request: UpdateMemberPointTradeConfigRequest) =
        configLogic.updatePointTradeConfig(request)

    @Get("/get")
    @Permission("member:config:query")
    suspend fun get(): MemberPointTradeConfigVO = configLogic.getPointTradeConfig()
}
