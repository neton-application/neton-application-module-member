package logic

import controller.admin.member.dto.CreateMemberTagRequest
import controller.admin.member.dto.UpdateMemberTagRequest
import dto.PageResponse
import model.MemberTag
import table.MemberTagTable
import neton.core.http.NotFoundException
import neton.database.dsl.*

import neton.logging.Logger

class MemberTagLogic(
    private val log: Logger
) {

    suspend fun create(request: CreateMemberTagRequest): Long {
        return MemberTagTable.insert(MemberTag(name = request.name)).id
    }

    suspend fun update(request: UpdateMemberTagRequest) {
        MemberTagTable.get(request.id)
            ?: throw NotFoundException("Member tag not found")
        MemberTagTable.update(MemberTag(id = request.id, name = request.name))
    }

    suspend fun delete(id: Long) {
        MemberTagTable.get(id)
            ?: throw NotFoundException("Member tag not found")
        MemberTagTable.destroy(id)
    }

    suspend fun getById(id: Long): MemberTag? = MemberTagTable.get(id)

    suspend fun list(): List<MemberTag> {
        return MemberTagTable.query {
            orderBy(MemberTag::id.desc())
        }.list()
    }

    suspend fun listAllSimple(): List<MemberTag> {
        return MemberTagTable.query {
            orderBy(MemberTag::id.desc())
        }.list()
    }

    suspend fun page(
        page: Int,
        size: Int,
        name: String? = null
    ): PageResponse<MemberTag> {
        val result = MemberTagTable.query {
            where {
                whenNotBlank(name) { MemberTag::name like "%$it%" }
            }
            orderBy(MemberTag::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }
}
