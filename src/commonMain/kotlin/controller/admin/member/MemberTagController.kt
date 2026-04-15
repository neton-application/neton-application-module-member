package controller.admin.member

import controller.admin.member.dto.CreateMemberTagRequest
import controller.admin.member.dto.UpdateMemberTagRequest
import logic.MemberTagLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/tag")
class MemberTagController(
    private val tagLogic: MemberTagLogic
) {

    @Post("/create")
    @Permission("member:tag:create")
    suspend fun create(@Body request: CreateMemberTagRequest): Long = tagLogic.create(request)

    @Put("/update")
    @Permission("member:tag:update")
    suspend fun update(@Body request: UpdateMemberTagRequest) = tagLogic.update(request)

    @Delete("/delete/{id}")
    @Permission("member:tag:delete")
    suspend fun delete(@PathVariable id: Long) = tagLogic.delete(id)

    @Get("/get/{id}")
    @Permission("member:tag:query")
    suspend fun get(@PathVariable id: Long) = tagLogic.getById(id)

    @Get("/list")
    @Permission("member:tag:list")
    suspend fun list() = tagLogic.list()

    @Get("/list-all-simple")
    @Permission("member:tag:list")
    suspend fun listAllSimple() = tagLogic.listAllSimple()

    @Get("/page")
    @Permission("member:tag:page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null
    ) = tagLogic.page(pageNo, pageSize, name)
}
