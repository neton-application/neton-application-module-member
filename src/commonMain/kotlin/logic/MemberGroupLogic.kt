package logic

import controller.admin.member.dto.CreateMemberGroupRequest
import controller.admin.member.dto.UpdateMemberGroupRequest
import dto.PageResponse
import model.MemberGroup
import table.MemberGroupTable
import neton.core.http.NotFoundException
import neton.database.dsl.*

import neton.logging.Logger

class MemberGroupLogic(
    private val log: Logger
) {

    suspend fun create(request: CreateMemberGroupRequest): Long {
        return MemberGroupTable.insert(
            MemberGroup(
                name = request.name,
                remark = request.remark,
                status = request.status
            )
        ).id
    }

    suspend fun update(request: UpdateMemberGroupRequest) {
        MemberGroupTable.get(request.id)
            ?: throw NotFoundException("Member group not found")
        MemberGroupTable.update(
            MemberGroup(
                id = request.id,
                name = request.name,
                remark = request.remark,
                status = request.status
            )
        )
    }

    suspend fun delete(id: Long) {
        MemberGroupTable.get(id)
            ?: throw NotFoundException("Member group not found")
        MemberGroupTable.destroy(id)
    }

    suspend fun getById(id: Long): MemberGroup? = MemberGroupTable.get(id)

    suspend fun page(
        page: Int,
        size: Int,
        name: String? = null,
        status: Int? = null
    ): PageResponse<MemberGroup> {
        val result = MemberGroupTable.query {
            where {
                and(
                    whenNotBlank(name) { MemberGroup::name like "%$it%" },
                    whenPresent(status) { MemberGroup::status eq it }
                )
            }
            orderBy(MemberGroup::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun listAllSimple(): List<MemberGroup> {
        return MemberGroupTable.query {
            where { MemberGroup::status eq 1 }
            orderBy(MemberGroup::id.desc())
        }.list()
    }
}
