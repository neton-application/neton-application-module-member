package setting

/**
 * member 模块的全局配置定义（SYSTEM_CONFIG_SPEC）。
 *
 * 配置归 `module-system`，而 member 本来就依赖 system，所以这里直接用类型化的
 * [SettingDefinition]，读取方拿定义去读即可 —— 不再需要中间的端口与适配器。
 */
object MemberSettingKeys {

    const val CATEGORY = "member"

    /**
     * 连续签到的周期长度（天）。
     *
     * 签到按 `(连续天数 % N) + 1` 取当天的奖励配置，即第 N+1 天回到第 1 天重新开始。
     * 上限 365 与签到配置 `day` 的 `@Max(365)` 对齐：超出周期的配置永远命中不了，
     * 与其让人配了不生效，不如在写入时就挡掉。
     */
    val SIGN_IN_CYCLE_DAYS: SettingDefinition<Int> = SettingDefinition.int(
        category = CATEGORY,
        key = "member.signin.cycle_days",
        default = 30,
        name = "连续签到周期(天)",
        description = "签到奖励按周期循环：第 N+1 天回到第 1 天。签到配置的天数必须在 1..N 之间。",
        min = 1,
        max = 365,
    )

    /**
     * 周期到头之后是否回到第 1 天重新循环。
     *
     * 关闭时不是「停发」而是**封顶**：连续签到超过周期后，每天固定拿第 N 天的奖励。
     * 停发会制造一个反向激励 —— 用户只能靠故意断签把连续天数清零才能重新拿到奖励，
     * 这跟签到想要的每日活跃恰好相反。要「不再发」，把第 N 天的奖励配成 0 即可，
     * 那是配置能表达的事，不需要再开一个语义。
     */
    val SIGN_IN_CYCLE_ENABLED: SettingDefinition<Boolean> = SettingDefinition.boolean(
        category = CATEGORY,
        key = "member.signin.cycle_enabled",
        default = true,
        name = "周期结束后循环",
        description = "开启：第 N+1 天回到第 1 天重新开始。关闭：连续签到超过 N 天后，之后每天都按第 N 天发放。",
    )

    /** 交给装配层聚合进 `SettingDefinitionRegistry`。 */
    val definitions: List<SettingDefinition<*>> = listOf(
        SIGN_IN_CYCLE_DAYS,
        SIGN_IN_CYCLE_ENABLED,
    )
}
