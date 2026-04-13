package controller.admin.level

import controller.admin.level.dto.CreateMemberLevelRequest
import controller.admin.level.dto.UpdateMemberLevelRequest
import logic.MemberLevelLogic
import model.MemberLevel
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable

@Controller("/member/level")
class MemberLevelController(
    private val memberLevelLogic: MemberLevelLogic
) {

    @Post("/create")
    suspend fun create(@Body request: CreateMemberLevelRequest): Long {
        return memberLevelLogic.create(
            MemberLevel(
                name = request.name,
                level = request.level,
                experience = request.experience,
                discount = request.discount,
                icon = request.icon,
                status = request.status
            )
        )
    }

    @Put("/update")
    suspend fun update(@Body request: UpdateMemberLevelRequest) {
        memberLevelLogic.update(
            MemberLevel(
                id = request.id,
                name = request.name,
                level = request.level,
                experience = request.experience,
                discount = request.discount,
                icon = request.icon,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    suspend fun delete(@PathVariable id: Long) {
        memberLevelLogic.delete(id)
    }

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): MemberLevel? {
        return memberLevelLogic.get(id)
    }

    @Get("/list")
    suspend fun list(): List<MemberLevel> {
        return memberLevelLogic.list()
    }

    @Get("/list-all-simple")
    suspend fun listAllSimple(): List<MemberLevel> {
        return memberLevelLogic.listAllSimple()
    }
}
