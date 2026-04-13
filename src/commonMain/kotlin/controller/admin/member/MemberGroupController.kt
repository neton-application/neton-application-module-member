package controller.admin.member

import controller.admin.member.dto.CreateMemberGroupRequest
import controller.admin.member.dto.UpdateMemberGroupRequest
import model.MemberGroup
import table.MemberGroupTable
import neton.database.dsl.*

import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/group")
class MemberGroupController {

    @Post("/create")
    suspend fun create(@Body request: CreateMemberGroupRequest): Long {
        return MemberGroupTable.insert(
            MemberGroup(
                name = request.name,
                remark = request.remark,
                status = request.status
            )
        ).id
    }

    @Put("/update")
    suspend fun update(@Body request: UpdateMemberGroupRequest) {
        MemberGroupTable.update(
            MemberGroup(
                id = request.id,
                name = request.name,
                remark = request.remark,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    suspend fun delete(@PathVariable id: Long) {
        MemberGroupTable.destroy(id)
    }

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): MemberGroup? {
        return MemberGroupTable.get(id)
    }

    @Get("/page")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query name: String? = null,
        @Query status: Int? = null
    ) = MemberGroupTable.query {
        where {
            and(
                whenNotBlank(name) { MemberGroup::name like "%$it%" },
                whenPresent(status) { MemberGroup::status eq it }
            )
        }
        orderBy(MemberGroup::id.desc())
    }.page(pageNo, pageSize)

    @Get("/list-all-simple")
    suspend fun listAllSimple(): List<MemberGroup> {
        return MemberGroupTable.query {
            where {
                MemberGroup::status eq 0
            }
            orderBy(MemberGroup::id.desc())
        }.list()
    }
}
