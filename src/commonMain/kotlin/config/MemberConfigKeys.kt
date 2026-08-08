package config

/**
 * member 模块的全局配置定义（SYSTEM_CONFIG_SPEC）。
 *
 * 配置归 `module-system`，而 member 本来就依赖 system，所以这里直接用类型化的
 * [ConfigDefinition]，读取方拿定义去读即可 —— 不再需要中间的端口与适配器。
 */
object MemberConfigKeys {

    const val CATEGORY = "member"

    /**
     * 连续签到的周期长度（天）。
     *
     * 签到按 `(连续天数 % N) + 1` 取当天的奖励配置，即第 N+1 天回到第 1 天重新开始。
     * 上限 365 与签到配置 `day` 的 `@Max(365)` 对齐：超出周期的配置永远命中不了，
     * 与其让人配了不生效，不如在写入时就挡掉。
     */
    val SIGN_IN_CYCLE_DAYS: ConfigDefinition<Int> = ConfigDefinition.int(
        category = CATEGORY,
        key = "member.signin.cycle_days",
        default = 30,
        name = "连续签到周期(天)",
        description = "签到奖励按周期循环：第 N+1 天回到第 1 天。签到配置的天数必须在 1..N 之间。",
        min = 1,
        max = 365,
    )

    /** 交给装配层聚合进 `ConfigDefinitionRegistry`。 */
    val definitions: List<ConfigDefinition<*>> = listOf(SIGN_IN_CYCLE_DAYS)
}
