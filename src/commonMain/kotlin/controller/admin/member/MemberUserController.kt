package controller.admin.member

import controller.admin.member.dto.UserUpdateLevelReqVO
import controller.admin.member.dto.UserPointUpdateReqVO
import logic.MemberLogic
import model.Member
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Put
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic
) {

    @Put("/update")
    suspend fun update(@Body member: Member) {
        memberLogic.update(member)
    }

    @Put("/update-level")
    suspend fun updateLevel(@Body req: UserUpdateLevelReqVO) {
        memberLogic.updateLevel(req.id, req.levelId, 0, null)
    }

    @Put("/update-point")
    suspend fun updatePoint(@Body req: UserPointUpdateReqVO) {
        memberLogic.updatePoint(req.id, req.point, 1, "Admin adjust", null)
    }

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): Member? {
        return memberLogic.get(id)
    }

    @Get("/page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query nickname: String? = null,
        @Query mobile: String? = null,
        @Query status: Int? = null,
        @Query levelId: Long? = null,
        @Query groupId: Long? = null
    ) = memberLogic.page(pageNo, pageSize, nickname, mobile, status, levelId, groupId)
}
