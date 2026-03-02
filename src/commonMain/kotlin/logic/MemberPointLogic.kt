package logic

import dto.PageResponse
import model.MemberPointRecord
import table.MemberPointRecordTable
import neton.database.dsl.*

import neton.logging.Logger

class MemberPointLogic(
    private val log: Logger
) {

    suspend fun pagePointRecords(
        page: Int,
        size: Int,
        userId: Long? = null,
        bizType: Int? = null,
        title: String? = null
    ): PageResponse<MemberPointRecord> {
        val result = MemberPointRecordTable.query {
            where {
                and(
                    whenPresent(userId) { MemberPointRecord::userId eq it },
                    whenPresent(bizType) { MemberPointRecord::bizType eq it },
                    whenNotBlank(title) { MemberPointRecord::title like "%$it%" }
                )
            }
            orderBy(MemberPointRecord::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun pageExperienceRecords(
        page: Int,
        size: Int,
        userId: Long? = null
    ): PageResponse<MemberPointRecord> {
        val result = MemberPointRecordTable.query {
            where {
                and(
                    whenPresent(userId) { MemberPointRecord::userId eq it },
                    // Experience records use a specific bizType
                    MemberPointRecord::bizType eq 100
                )
            }
            orderBy(MemberPointRecord::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun getPointRecord(id: Long): MemberPointRecord? {
        return MemberPointRecordTable.get(id)
    }
}
