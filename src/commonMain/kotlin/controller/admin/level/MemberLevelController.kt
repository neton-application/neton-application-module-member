package controller.admin.level

import controller.admin.level.dto.CreateMemberLevelRequest
import controller.admin.level.dto.UpdateMemberLevelRequest
import logic.MemberLevelLogic
import model.MemberLevel
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
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
    @Permission("member:level:create")
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
    @Permission("member:level:update")
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
    @Permission("member:level:delete")
    suspend fun delete(@PathVariable id: Long) {
        memberLevelLogic.delete(id)
    }

    @Get("/get/{id}")
    @Permission("member:level:query")
    suspend fun get(@PathVariable id: Long): MemberLevel? {
        return memberLevelLogic.get(id)
    }

    @Get("/list")
    @Permission("member:level:list")
    suspend fun list(): List<MemberLevel> {
        return memberLevelLogic.list()
    }

    @Get("/list-all-simple")
    @Permission("member:level:list")
    suspend fun listAllSimple(): List<MemberLevel> {
        return memberLevelLogic.listAllSimple()
    }
}
