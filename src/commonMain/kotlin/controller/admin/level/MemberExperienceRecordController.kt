package controller.admin.level

import logic.MemberPointLogic
import model.MemberPointRecord
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/experience-record")
class MemberExperienceRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query userId: Long? = null
    ) = memberPointLogic.pageExperienceRecords(pageNo, pageSize, userId)

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): MemberPointRecord? {
        return memberPointLogic.getPointRecord(id)
    }
}
