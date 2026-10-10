package logic

import kotlin.time.Clock
import model.MemberLevelRecord
import model.MemberPointRecord
import model.MemberProfile
import table.MemberLevelRecordTable
import table.MemberPointRecordTable
import table.MemberProfileTable
import neton.database.dsl.*
import neton.database.api.DbContext
import neton.logging.Logger

/**
 * 会员档案的读写入口：等级、经验、积分、分组。
 *
 * 余额与经验**只做原子更新**（`SET point = point + :n`），不做「读出来、加上、写回去」。
 * 后者在并发下会丢更新：两次发奖同时读到旧值、各自写回，用户只拿到一次。
 *
 * 档案行惰性创建：第一次写入前用 `INSERT ... ON CONFLICT DO NOTHING` 补一行。
 */
@neton.core.annotations.Logic(logger = "logic.member")
class MemberLogic(
    private val log: Logger,
    private val db: DbContext,
) {

    /** 读档案。没有档案（从没产生过成长数据）按全默认值返回，不建行。 */
    suspend fun profile(userId: Long): MemberProfile =
        MemberProfileTable.oneWhere { MemberProfile::userId eq userId } ?: MemberProfile(userId = userId)

    /** 批量读档案，缺失的按默认值补齐。后台列表一页一次查询，不做 N+1。 */
    suspend fun profiles(userIds: Collection<Long>): Map<Long, MemberProfile> {
        if (userIds.isEmpty()) return emptyMap()
        val found = MemberProfileTable.query { where { MemberProfile::userId `in` userIds.toList() } }
            .list()
            .associateBy { it.userId }
        return userIds.associateWith { found[it] ?: MemberProfile(userId = it) }
    }

    suspend fun updateLevel(userId: Long, levelId: Long, level: Int, reason: String?) {
        db.transaction {
            ensure(userId)
            db.execute(
                "UPDATE member_profiles SET level_id = :levelId, updated_at = :now WHERE user_id = :userId",
                mapOf("levelId" to levelId, "userId" to userId, "now" to now()),
            )
            MemberLevelRecordTable.insert(MemberLevelRecord(
                userId = userId,
                levelId = levelId,
                level = level,
                reason = reason,
                description = "Admin updated member level",
            ))
        }
        log.info("Updated member level: userId=$userId, levelId=$levelId, level=$level")
    }

    suspend fun updateGroup(userId: Long, groupId: Long?) {
        db.transaction {
            ensure(userId)
            db.execute(
                "UPDATE member_profiles SET group_id = :groupId, updated_at = :now WHERE user_id = :userId",
                mapOf("groupId" to groupId, "userId" to userId, "now" to now()),
            )
        }
        log.info("Updated member group: userId=$userId, groupId=$groupId")
    }

    /**
     * 变更积分（正数加、负数减）并写流水，同一事务。
     *
     * 减到负数是允许的 —— 这是管理员手工调账的语义（冲正）。业务扣减一律走 [debitPoint]，
     * 那条路径余额不足会拒绝。
     *
     * 在调用方的事务里调用时跟随调用方提交 / 回滚（签到把记录、积分、经验、现金放在一个事务里）。
     *
     * @return 变更后的余额
     */
    suspend fun changePoint(
        userId: Long,
        point: Int,
        bizType: Int,
        title: String,
        description: String? = null,
        bizId: String? = null,
    ): Int {
        var total = 0
        db.transaction {
            ensure(userId)
            db.execute(
                "UPDATE member_profiles SET point = point + :n, updated_at = :now WHERE user_id = :userId",
                mapOf("n" to point, "userId" to userId, "now" to now()),
            )
            total = profile(userId).point
            MemberPointRecordTable.insert(MemberPointRecord(
                userId = userId,
                bizType = bizType,
                bizId = bizId,
                title = title,
                point = point,
                totalPoint = total,
                description = description,
            ))
        }
        log.info("Changed member point: userId=$userId, point=$point, totalPoint=$total")
        return total
    }

    /**
     * 扣积分，余额不足则拒绝。
     *
     * 条件更新让数据库裁决谁赢：`... WHERE point >= :n`。影响行数为 0 = 当时余额不足，什么都不写。
     * 两次并发兑换各自读到同一个余额再写回 —— 那样会一份钱买两样东西，或者余额变负。
     *
     * @return false = 余额不足；true = 已扣减并写流水
     */
    suspend fun debitPoint(userId: Long, point: Int, bizType: Int, title: String, description: String?): Boolean {
        require(point > 0) { "debit must be positive, got $point" }
        var debited = false
        db.transaction {
            val affected = db.execute(
                "UPDATE member_profiles SET point = point - :n, updated_at = :now WHERE user_id = :userId AND point >= :n",
                mapOf("n" to point, "userId" to userId, "now" to now()),
            )
            if (affected == 1L) {
                MemberPointRecordTable.insert(MemberPointRecord(
                    userId = userId,
                    bizType = bizType,
                    title = title,
                    point = -point,
                    totalPoint = profile(userId).point,
                    description = description,
                ))
                debited = true
            }
        }
        if (debited) log.info("Debited member point: userId=$userId, point=$point, bizType=$bizType")
        return debited
    }

    /** 加经验。经验只增不减。 */
    suspend fun addExperience(userId: Long, experience: Int): Long {
        require(experience >= 0) { "experience must not be negative, got $experience" }
        var total = 0L
        db.transaction {
            ensure(userId)
            db.execute(
                "UPDATE member_profiles SET experience = experience + :n, updated_at = :now WHERE user_id = :userId",
                mapOf("n" to experience, "userId" to userId, "now" to now()),
            )
            total = profile(userId).experience
        }
        return total
    }

    private suspend fun ensure(userId: Long) {
        db.execute(
            "INSERT INTO member_profiles (user_id, created_at, updated_at) VALUES (:userId, :now, :now) " +
                "ON CONFLICT (user_id) DO NOTHING",
            mapOf("userId" to userId, "now" to now()),
        )
    }

    private fun now(): Long = Clock.System.now().toEpochMilliseconds()
}
