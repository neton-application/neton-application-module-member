package logic

import dto.PageResponse
import model.Member
import model.MemberSignInConfig
import model.MemberSignInRecord
import model.MemberPointRecord
import table.MemberTable
import table.MemberSignInConfigTable
import table.MemberSignInRecordTable
import table.MemberPointRecordTable
import controller.admin.signin.dto.MemberSignInSummaryVO
import neton.database.dsl.*

import neton.logging.Logger
import neton.core.http.BadRequestException
import kotlin.time.Clock

class MemberSignInLogic(
    private val log: Logger
) {

    // --- Sign-in config CRUD (admin) ---

    suspend fun createConfig(config: MemberSignInConfig): Long {
        val inserted = MemberSignInConfigTable.insert(config)
        log.info("Created sign-in config: id=${inserted.id}, day=${config.day}")
        return inserted.id
    }

    suspend fun updateConfig(config: MemberSignInConfig) {
        MemberSignInConfigTable.update(config)
        log.info("Updated sign-in config: id=${config.id}")
    }

    suspend fun deleteConfig(id: Long) {
        MemberSignInConfigTable.destroy(id)
        log.info("Deleted sign-in config: id=$id")
    }

    suspend fun getConfig(id: Long): MemberSignInConfig? {
        return MemberSignInConfigTable.get(id)
    }

    suspend fun listConfigs(): List<MemberSignInConfig> {
        return MemberSignInConfigTable.query {
            where {
                MemberSignInConfig::status eq 0
            }
            orderBy(MemberSignInConfig::day.asc())
        }.list()
    }

    // --- Sign-in records ---

    suspend fun signIn(userId: Long): MemberSignInRecord {
        // Get today's sign-in summary to determine continuous days
        val summary = getSummary(userId)
        if (summary.todaySigned) {
            throw BadRequestException("Already signed in today")
        }

        val nextDay = summary.continuousDay + 1

        // Find matching config for the day
        val config = MemberSignInConfigTable.oneWhere {
            and(
                MemberSignInConfig::day eq nextDay,
                MemberSignInConfig::status eq 0
            )
        }

        val point = config?.point ?: 0
        val experience = config?.experience ?: 0

        // Create sign-in record
        val record = MemberSignInRecord(
            userId = userId,
            day = nextDay,
            point = point,
            experience = experience
        )
        val insertedRecord = MemberSignInRecordTable.insert(record)

        // Award points if applicable
        if (point > 0) {
            val member = MemberTable.get(userId)
            if (member != null) {
                val newPoint = member.point + point
                MemberTable.update(member.copy(point = newPoint))

                val pointRecord = MemberPointRecord(
                    userId = userId,
                    bizType = 1, // Sign-in type
                    bizId = insertedRecord.id.toString(),
                    title = "Sign-in reward (Day $nextDay)",
                    point = point,
                    totalPoint = newPoint,
                    description = "Daily sign-in reward"
                )
                MemberPointRecordTable.insert(pointRecord)
            }
        }

        // Award experience if applicable
        if (experience > 0) {
            val member = MemberTable.get(userId)
            if (member != null) {
                val newExperience = member.experience + experience
                MemberTable.update(member.copy(experience = newExperience))
            }
        }

        log.info("Member signed in: userId=$userId, day=$nextDay, point=$point, experience=$experience")

        return insertedRecord
    }

    suspend fun pageRecords(
        page: Int,
        size: Int,
        userId: Long? = null
    ): PageResponse<MemberSignInRecord> {
        val result = MemberSignInRecordTable.query {
            where {
                whenPresent(userId) { MemberSignInRecord::userId eq it }
            }
            orderBy(MemberSignInRecord::id.desc())
        }.page(page, size)
        return PageResponse(result.items, result.total, page, size,
            if (size > 0) ((result.total + size - 1) / size).toInt() else 0)
    }

    suspend fun getSummary(userId: Long): MemberSignInSummaryVO {
        val records = MemberSignInRecordTable.query {
            where {
                MemberSignInRecord::userId eq userId
            }
            orderBy(MemberSignInRecord::id.desc())
        }.list()

        val totalDay = records.size

        // Calculate continuous days by checking consecutive dates
        val now = Clock.System.now()
        val todayEpochDay = now.toEpochMilliseconds() / 86400000L  // ms to days

        var continuousDay = 0
        var checkEpochDay = todayEpochDay - 1  // Start checking from yesterday backwards

        // Check if signed in today first
        val todaySigned = records.any { record ->
            val recordCreatedAt = record.createdAt
            if (recordCreatedAt != null) {
                val recordEpochDay = try {
                    recordCreatedAt.toLong() / 86400000L
                } catch (_: Exception) {
                    -1L
                }
                recordEpochDay == todayEpochDay
            } else false
        }

        // If signed in today, include today in continuous count
        if (todaySigned) {
            continuousDay = 1
            checkEpochDay = todayEpochDay - 1
        }

        // Count consecutive days backwards
        for (record in records) {
            val recordCreatedAt = record.createdAt ?: continue
            val recordEpochDay = try {
                recordCreatedAt.toLong() / 86400000L
            } catch (_: Exception) {
                continue
            }

            if (recordEpochDay == todayEpochDay && todaySigned) {
                // Already counted today
                continue
            }
            if (recordEpochDay == checkEpochDay) {
                continuousDay++
                checkEpochDay--
            } else if (recordEpochDay < checkEpochDay) {
                // Gap found, stop counting
                break
            }
        }

        return MemberSignInSummaryVO(
            totalDay = totalDay,
            continuousDay = continuousDay,
            todaySigned = todaySigned
        )
    }
}
