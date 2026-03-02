package logic

import dto.PageResponse
import model.MemberLevel
import model.MemberLevelRecord
import table.MemberLevelTable
import table.MemberLevelRecordTable
import neton.database.dsl.*

import neton.logging.Logger

class MemberLevelLogic(
    private val log: Logger
) {

    suspend fun create(level: MemberLevel): Long {
        val inserted = MemberLevelTable.insert(level)
        log.info("Created member level: id=${inserted.id}, name=${level.name}")
        return inserted.id
    }

    suspend fun update(level: MemberLevel) {
        MemberLevelTable.update(level)
        log.info("Updated member level: id=${level.id}")
    }

    suspend fun delete(id: Long) {
        MemberLevelTable.destroy(id)
        log.info("Deleted member level: id=$id")
    }

    suspend fun get(id: Long): MemberLevel? {
        return MemberLevelTable.get(id)
    }

    suspend fun list(): List<MemberLevel> {
        return MemberLevelTable.query {
            orderBy(MemberLevel::level.asc())
        }.list()
    }

    suspend fun listAllSimple(): List<MemberLevel> {
        return MemberLevelTable.query {
            where {
                MemberLevel::status eq 0
            }
            orderBy(MemberLevel::level.asc())
        }.list()
    }

    suspend fun listForApp(): List<MemberLevel> {
        return MemberLevelTable.query {
            where {
                MemberLevel::status eq 0
            }
            orderBy(MemberLevel::level.asc())
        }.list()
    }

    suspend fun pageLevelRecords(
        page: Int,
        size: Int,
        userId: Long? = null,
        levelId: Long? = null
    ): PageResponse<MemberLevelRecord> {
        val result = MemberLevelRecordTable.query {
            where {
                and(
                    whenPresent(userId) { MemberLevelRecord::userId eq it },
                    whenPresent(levelId) { MemberLevelRecord::levelId eq it }
                )
            }
            orderBy(MemberLevelRecord::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun getLevelRecord(id: Long): MemberLevelRecord? {
        return MemberLevelRecordTable.get(id)
    }
}
