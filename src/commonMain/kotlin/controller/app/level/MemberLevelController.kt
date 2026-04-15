package controller.app.level

import logic.MemberLevelLogic
import model.MemberLevel
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/app/member/level")
class MemberLevelController(
    private val memberLevelLogic: MemberLevelLogic
) {

    @Get("/list")
    suspend fun list(): List<MemberLevel> {
        return memberLevelLogic.listForApp()
    }
}
