package controller.app.level

import logic.MemberPointLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/app/member/experience-record")
class MemberExperienceRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        userId: Long,
        pageNo: Int = 1,
        pageSize: Int = 10
    ) = memberPointLogic.pageExperienceRecords(pageNo, pageSize, userId)
}
