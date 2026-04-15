package controller.app.point

import logic.MemberPointLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/app/member/point/record")
class MemberPointRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        userId: Long,
        pageNo: Int = 1,
        pageSize: Int = 10,
        bizType: Int? = null,
        title: String? = null
    ) = memberPointLogic.pagePointRecords(pageNo, pageSize, userId, bizType, title)
}
