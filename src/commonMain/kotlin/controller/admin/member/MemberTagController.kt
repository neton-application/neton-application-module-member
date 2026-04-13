package controller.admin.member

import controller.admin.member.dto.CreateMemberTagRequest
import controller.admin.member.dto.UpdateMemberTagRequest
import model.MemberTag
import table.MemberTagTable
import neton.database.dsl.*

import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/tag")
class MemberTagController {

    @Post("/create")
    suspend fun create(@Body request: CreateMemberTagRequest): Long {
        return MemberTagTable.insert(MemberTag(name = request.name)).id
    }

    @Put("/update")
    suspend fun update(@Body request: UpdateMemberTagRequest) {
        MemberTagTable.update(MemberTag(id = request.id, name = request.name))
    }

    @Delete("/delete/{id}")
    suspend fun delete(@PathVariable id: Long) {
        MemberTagTable.destroy(id)
    }

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): MemberTag? {
        return MemberTagTable.get(id)
    }

    @Get("/list")
    suspend fun list(): List<MemberTag> {
        return MemberTagTable.query {
            orderBy(MemberTag::id.desc())
        }.list()
    }

    @Get("/list-all-simple")
    suspend fun listAllSimple(): List<MemberTag> {
        return MemberTagTable.query {
            orderBy(MemberTag::id.desc())
        }.list()
    }

    @Get("/page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null
    ) = MemberTagTable.query {
        where {
            whenNotBlank(name) { MemberTag::name like "%$it%" }
        }
        orderBy(MemberTag::id.desc())
    }.page(pageNo, pageSize)
}
