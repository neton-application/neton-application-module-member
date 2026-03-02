package controller.admin.signin

import logic.MemberSignInLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get

@Controller("/member/sign-in/record")
class MemberSignInRecordController(
    private val memberSignInLogic: MemberSignInLogic
) {

    @Get("/page")
    suspend fun page(
        pageNo: Int = 1,
        pageSize: Int = 10,
        userId: Long? = null
    ) = memberSignInLogic.pageRecords(pageNo, pageSize, userId)
}
