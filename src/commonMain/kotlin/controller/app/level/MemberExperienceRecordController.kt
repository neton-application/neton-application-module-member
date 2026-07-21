package controller.app.level

import logic.MemberPointLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Query
import neton.core.interfaces.Identity

@Controller("/member/experience-record")
class MemberExperienceRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        identity: Identity,
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10
    ) = memberPointLogic.pageExperienceRecords(pageNo, pageSize, identity.id.toLong())
}
