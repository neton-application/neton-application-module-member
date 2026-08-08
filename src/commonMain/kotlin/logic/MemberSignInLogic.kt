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
import controller.admin.signin.dto.MemberSignInRecordVO
import controller.admin.signin.dto.MemberSignInSummaryVO
import neton.database.dsl.*
import neton.database.api.DbContext

import neton.logging.Logger
import neton.core.http.BadRequestException
import kotlin.time.Clock

// 注意：不用 @Logic ——本类需要注入可选的 [port.MemberRewardPort]（KSP Logic 装配只支持
// log/db 固定依赖），改为在 [init.MemberRuntimeBootstrap] 手动装配（与 MemberProfileLogic
// 的 identityAdapter 同款 port 注入模式）。
class MemberSignInLogic(
    private val log: Logger,
    private val db: DbContext,
    /**
     * 签到现金奖励发放端口（可空）：产品装配层（如 PrivChat）bind 后，
     * `cashAmount > 0` 的签到在事务内经此发放现金；未装配时配置了现金即签到失败（防静默不发）。
     */
    private val rewardPort: port.MemberRewardPort? = null,
    /**
     * 全局设置读取端口（可空）：装配后「连续签到周期」可在后台调整；
     * 未装配时一律用 [setting.MemberSettingKeys.SIGN_IN_CYCLE_DAYS_DEFAULT]。
     */
    private val configs: logic.SystemSettingLogic? = null,
) {

    /**
     * 当前生效的连续签到周期（天）。
     *
     * 默认值与越界回退由配置定义本身保证（[MemberSettingKeys.SIGN_IN_CYCLE_DAYS] 声明了
     * `min=1, max=365`），所以这里不再重复防御——读到的一定是可用的正整数，
     * `% cycleDays` 不会除零。
     *
     * `configs` 为空只发生在未装配 system 的测试装配里，此时用定义的默认值。
     */
    private suspend fun cycleDays(): Int =
        configs?.get(setting.MemberSettingKeys.SIGN_IN_CYCLE_DAYS)
            ?: setting.MemberSettingKeys.SIGN_IN_CYCLE_DAYS.default

    // --- Sign-in config CRUD (admin) ---

    /**
     * 保存前的公共校验。
     *
     * 两条都是「配了也永远不会生效」的情况，不拦住的话运营配完看不出任何异常，
     * 只有用户签到时才发现奖励不对：
     *
     * - `day` 超出周期：签到取的是 `(连续天数 % 周期) + 1`，落不到这一天上。
     * - `day` 重复：命中用的是 `oneWhere`，两条同 day 且启用时取哪条是不确定的
     *   （生产上出过一条 10 元、一条 0 分共存，第 7 天发多少全看运气）。
     */
    private suspend fun validateConfig(config: MemberSignInConfig, excludeId: Long?) {
        val cycle = cycleDays()
        if (config.day !in 1..cycle) {
            throw BadRequestException(
                "签到天数必须在 1..$cycle 之间（当前连续签到周期为 $cycle 天）"
            )
        }
        val duplicated = MemberSignInConfigTable
            .query { where { MemberSignInConfig::day eq config.day } }
            .list()
            .any { row -> row.id != excludeId }
        if (duplicated) {
            throw BadRequestException("第 ${config.day} 天已存在配置，请勿重复添加")
        }
    }

    suspend fun createConfig(config: MemberSignInConfig): Long {
        validateConfig(config, excludeId = null)
        val inserted = MemberSignInConfigTable.insert(config)
        log.info("Created sign-in config: id=${inserted.id}, day=${config.day}")
        return inserted.id
    }

    suspend fun updateConfig(config: MemberSignInConfig) {
        val existing = MemberSignInConfigTable.get(config.id)
            ?: throw BadRequestException("Sign-in config not found: id=${config.id}")
        validateConfig(config, excludeId = config.id)
        // 全列 update：请求构造的 config 不带时间戳，必须保留原行 created_at，
        // 否则写 null 触发 23502 非空约束（与 MenuLogic.update 同款修复）。
        MemberSignInConfigTable.update(
            config.copy(createdAt = existing.createdAt, updatedAt = existing.updatedAt)
        )
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
                MemberSignInConfig::status eq 1
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

        // 周期循环：第 N+1 天回到第 1 天。
        //
        // 取模而不是无限递增——原先 nextDay 一直往上涨，配置只到第 N 天，于是第 N+1 天起
        // 查不到配置、奖励恒为 0，用户只能靠**故意断签**把连续天数清零才能重新拿奖励，
        // 这跟签到想要的每日活跃恰好相反。
        val cycle = cycleDays()
        val nextDay = (summary.continuousDay % cycle) + 1

        // Find matching config for the day
        val config = MemberSignInConfigTable.oneWhere {
            and(
                MemberSignInConfig::day eq nextDay,
                MemberSignInConfig::status eq 1
            )
        }

        val point = config?.point ?: 0
        val experience = config?.experience ?: 0
        val cashAmount = config?.cashAmount ?: 0L

        // 现金奖励已配置但产品未装配发放端口：直接失败（fail-fast），
        // 不允许「签到成功但现金静默没发」。
        if (cashAmount > 0 && rewardPort == null) {
            throw IllegalStateException(
                "Sign-in cash reward configured (day=$nextDay, cashAmount=$cashAmount) but no MemberRewardPort wired"
            )
        }

        // Create sign-in record + award points/experience/cash in a single transaction
        return db.transaction {
            // Create sign-in record
            val insertedRecord = MemberSignInRecordTable.insert(MemberSignInRecord(
                userId = userId,
                day = nextDay,
                point = point,
                experience = experience,
                cashAmount = cashAmount
            ))

            // Award points if applicable
            if (point > 0) {
                val member = MemberTable.get(userId)
                if (member != null) {
                    val newPoint = member.point + point
                    MemberTable.update(member.copy(point = newPoint))

                    MemberPointRecordTable.insert(MemberPointRecord(
                        userId = userId,
                        bizType = 1, // Sign-in type
                        bizId = insertedRecord.id.toString(),
                        title = "Sign-in reward (Day $nextDay)",
                        point = point,
                        totalPoint = newPoint,
                        description = "Daily sign-in reward"
                    ))
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

            // Award cash if applicable（同事务；实现方异常 → 整体回滚，签到可重试）
            if (cashAmount > 0) {
                checkNotNull(rewardPort).onSignInCashReward(
                    port.SignInCashRewardEvent(
                        userId = userId,
                        recordId = insertedRecord.id,
                        day = nextDay,
                        cashAmount = cashAmount,
                    )
                )
            }

            log.info("Member signed in: userId=$userId, day=$nextDay, point=$point, experience=$experience, cashAmount=$cashAmount")

            insertedRecord
        }
    }

    /**
     * 签到记录分页。返回 VO 而不是裸实体：实体里只有 userId，列表要显示的是人名，
     * 每行再让前端去查一次用户就成了 N+1。
     *
     * 昵称按本页出现的 userId 去重后一次性取回，所以补昵称是固定一条查询，与页大小无关。
     */
    suspend fun pageRecords(
        page: Int,
        size: Int,
        userId: Long? = null,
        day: Int? = null
    ): PageResponse<MemberSignInRecordVO> {
        val result = MemberSignInRecordTable.query {
            where {
                and(
                    whenPresent(userId) { MemberSignInRecord::userId eq it },
                    whenPresent(day) { MemberSignInRecord::day eq it },
                )
            }
            orderBy(MemberSignInRecord::id.desc())
        }.page(page, size)

        val nicknames: Map<Long, String> = result.items
            .map { it.userId }
            .distinct()
            .takeIf { it.isNotEmpty() }
            ?.let { ids ->
                MemberTable.query { where { Member::id `in` ids } }
                    .list()
                    .associate { it.id to it.nickname }
            }
            ?: emptyMap()

        val list = result.items.map { record ->
            MemberSignInRecordVO(
                id = record.id,
                userId = record.userId,
                nickname = nicknames[record.userId],
                day = record.day,
                point = record.point,
                experience = record.experience,
                cashAmount = record.cashAmount,
                createdAt = record.createdAt,
            )
        }

        return PageResponse(list, result.total, page, size,
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
