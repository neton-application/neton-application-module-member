package controller.app.signin

import logic.MemberSignInLogic
import model.MemberSignInConfig
import model.MemberSignInRecord
import controller.admin.signin.dto.MemberSignInSummaryVO
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post

@Controller("/member/sign-in")
class MemberSignInController(
    private val memberSignInLogic: MemberSignInLogic
) {

    @Get("/config/list")
    suspend fun listConfigs(): List<MemberSignInConfig> {
        return memberSignInLogic.listConfigs()
    }

    @Get("/record/get-summary")
    suspend fun getSummary(userId: Long): MemberSignInSummaryVO {
        return memberSignInLogic.getSummary(userId)
    }

    @Post("/record/create")
    suspend fun signIn(userId: Long): MemberSignInRecord {
        return memberSignInLogic.signIn(userId)
    }

    @Get("/record/page")
    suspend fun pageRecords(
        userId: Long,
        page: Int = 1,
        size: Int = 10
    ) = memberSignInLogic.pageRecords(page, size, userId)
}
