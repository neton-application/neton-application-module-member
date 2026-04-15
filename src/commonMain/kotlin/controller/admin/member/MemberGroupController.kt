package controller.admin.member

import controller.admin.member.dto.CreateMemberGroupRequest
import controller.admin.member.dto.UpdateMemberGroupRequest
import logic.MemberGroupLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/group")
class MemberGroupController(
    private val groupLogic: MemberGroupLogic
) {

    @Post("/create")
    @Permission("member:group:create")
    suspend fun create(@Body request: CreateMemberGroupRequest): Long = groupLogic.create(request)

    @Put("/update")
    @Permission("member:group:update")
    suspend fun update(@Body request: UpdateMemberGroupRequest) = groupLogic.update(request)

    @Delete("/delete/{id}")
    @Permission("member:group:delete")
    suspend fun delete(@PathVariable id: Long) = groupLogic.delete(id)

    @Get("/get/{id}")
    @Permission("member:group:query")
    suspend fun get(@PathVariable id: Long) = groupLogic.getById(id)

    @Get("/page")
    @Permission("member:group:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query status: Int? = null
    ) = groupLogic.page(pageNo, pageSize, name, status)

    @Get("/list-all-simple")
    @Permission("member:group:list")
    suspend fun listAllSimple() = groupLogic.listAllSimple()
}
