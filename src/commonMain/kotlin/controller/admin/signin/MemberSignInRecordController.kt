package controller.admin.signin

import logic.MemberSignInLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission

@Controller("/member/sign-in/record")
class MemberSignInRecordController(
    private val memberSignInLogic: MemberSignInLogic
) {

    // 同 config/list-all：app 侧已占 GET /member/sign-in/record/page，admin 换 pattern。
    @Get("/page-all")
    @Permission("member:signin:page")
    suspend fun page(
        pageNo: Int = 1,
        pageSize: Int = 10,
        userId: Long? = null
    ) = memberSignInLogic.pageRecords(pageNo, pageSize, userId)
}
