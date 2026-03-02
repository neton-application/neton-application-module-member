package controller.admin.level

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
    suspend fun create(@Body level: MemberLevel): Long {
        return memberLevelLogic.create(level)
    }

    @Put("/update")
    suspend fun update(@Body level: MemberLevel) {
        memberLevelLogic.update(level)
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
