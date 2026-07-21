package controller.app.signin

import logic.MemberSignInLogic
import model.MemberSignInConfig
import model.MemberSignInRecord
import controller.admin.signin.dto.MemberSignInSummaryVO
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Query
import neton.core.interfaces.Identity

@Controller("/member/sign-in")
class MemberSignInController(
    private val memberSignInLogic: MemberSignInLogic
) {

    @Get("/config/list")
    suspend fun listConfigs(): List<MemberSignInConfig> {
        return memberSignInLogic.listConfigs()
    }

    @Get("/record/get-summary")
    suspend fun getSummary(identity: Identity): MemberSignInSummaryVO {
        return memberSignInLogic.getSummary(identity.id.toLong())
    }

    @Post("/record/create")
    suspend fun signIn(identity: Identity): MemberSignInRecord {
        return memberSignInLogic.signIn(identity.id.toLong())
    }

    @Get("/record/page")
    suspend fun pageRecords(
        identity: Identity,
        @Query page: Int = 1,
        @Query size: Int = 10
    ) = memberSignInLogic.pageRecords(page, size, identity.id.toLong())
}
