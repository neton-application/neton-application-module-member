package controller.admin.member

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
    suspend fun create(@Body group: MemberGroup): Long {
        return MemberGroupTable.insert(group).id
    }

    @Put("/update")
    suspend fun update(@Body group: MemberGroup) {
        MemberGroupTable.update(group)
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
