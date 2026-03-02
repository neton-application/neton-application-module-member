package controller.admin.point

import logic.MemberPointLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/member/point/record")
class MemberPointRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        pageNo: Int = 1,
        pageSize: Int = 10,
        userId: Long? = null,
        bizType: Int? = null,
        title: String? = null
    ) = memberPointLogic.pagePointRecords(pageNo, pageSize, userId, bizType, title)
}
