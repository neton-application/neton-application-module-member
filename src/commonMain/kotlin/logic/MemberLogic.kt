package logic

import dto.PageResponse
import model.Member
import model.MemberLevelRecord
import model.MemberPointRecord
import table.MemberTable
import table.MemberLevelRecordTable
import table.MemberPointRecordTable
import neton.database.dsl.*

import neton.logging.Logger

class MemberLogic(
    private val log: Logger
) {

    suspend fun page(
        page: Int,
        size: Int,
        nickname: String? = null,
        mobile: String? = null,
        status: Int? = null,
        levelId: Long? = null,
        groupId: Long? = null
    ): PageResponse<Member> {
        val result = MemberTable.query {
            where {
                and(
                    whenNotBlank(nickname) { Member::nickname like "%$it%" },
                    whenNotBlank(mobile) { Member::mobile like "%$it%" },
                    whenPresent(status) { Member::status eq it },
                    whenPresent(levelId) { Member::levelId eq it },
                    whenPresent(groupId) { Member::groupId eq it }
                )
            }
            orderBy(Member::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun get(id: Long): Member? {
        return MemberTable.get(id)
    }

    suspend fun update(member: Member) {
        MemberTable.update(member)
        log.info("Updated member: id=${member.id}")
    }

    suspend fun updateLevel(userId: Long, levelId: Long, level: Int, reason: String?) {
        val member = MemberTable.get(userId)
            ?: throw IllegalArgumentException("Member not found: $userId")

        // Update member + create level record in a single transaction
        MemberTable.transaction {
            MemberTable.update(member.copy(levelId = levelId))

            MemberLevelRecordTable.insert(MemberLevelRecord(
                userId = userId,
                levelId = levelId,
                level = level,
                reason = reason,
                description = "Admin updated member level"
            ))
        }

        log.info("Updated member level: userId=$userId, levelId=$levelId, level=$level")
    }

    suspend fun updatePoint(userId: Long, point: Int, bizType: Int, title: String, description: String?) {
        val member = MemberTable.get(userId)
            ?: throw IllegalArgumentException("Member not found: $userId")

        val newPoint = member.point + point

        // Update member point + create point record in a single transaction
        MemberTable.transaction {
            MemberTable.update(member.copy(point = newPoint))

            MemberPointRecordTable.insert(MemberPointRecord(
                userId = userId,
                bizType = bizType,
                title = title,
                point = point,
                totalPoint = newPoint,
                description = description
            ))
        }

        log.info("Updated member point: userId=$userId, point=$point, totalPoint=$newPoint")
    }
}
