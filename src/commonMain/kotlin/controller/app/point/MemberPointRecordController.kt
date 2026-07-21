package controller.app.point

import logic.MemberPointLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Query
import neton.core.interfaces.Identity

@Controller("/member/point/record")
class MemberPointRecordController(
    private val memberPointLogic: MemberPointLogic
) {

    @Get("/page")
    suspend fun page(
        identity: Identity,
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query bizType: Int? = null,
        @Query title: String? = null
    ) = memberPointLogic.pagePointRecords(pageNo, pageSize, identity.id.toLong(), bizType, title)
}
