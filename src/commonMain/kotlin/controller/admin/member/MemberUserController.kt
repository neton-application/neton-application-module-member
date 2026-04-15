package controller.admin.member

import controller.admin.member.dto.UpdateMemberRequest
import controller.admin.member.dto.UpdateMemberUserLevelRequest
import controller.admin.member.dto.UpdateMemberUserPointRequest
import logic.MemberLogic
import model.Member
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Put
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic
) {

    @Put("/update")
    @Permission("member:user:update")
    suspend fun update(@Body request: UpdateMemberRequest) {
        val existing = memberLogic.get(request.id)
            ?: throw IllegalArgumentException("Member not found: ${request.id}")
        memberLogic.update(
            existing.copy(
                mobile = request.mobile,
                nickname = request.nickname,
                avatar = request.avatar,
                status = request.status,
                levelId = request.levelId,
                groupId = request.groupId
            )
        )
    }

    @Put("/update-level")
    @Permission("member:user:update")
    suspend fun updateLevel(@Body req: UpdateMemberUserLevelRequest) {
        memberLogic.updateLevel(req.id, req.levelId, 0, null)
    }

    @Put("/update-point")
    @Permission("member:user:update")
    suspend fun updatePoint(@Body req: UpdateMemberUserPointRequest) {
        memberLogic.updatePoint(req.id, req.point, 1, "Admin adjust", null)
    }

    @Get("/get/{id}")
    @Permission("member:user:query")
    suspend fun get(@PathVariable id: Long): Member? {
        return memberLogic.get(id)
    }

    @Get("/page")
    @Permission("member:user:page")
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
