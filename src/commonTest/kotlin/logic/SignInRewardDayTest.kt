package logic

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 「今天该按第几天发奖」的边界。
 *
 * 这里只测纯函数：入参是连续天数 / 周期 / 是否循环，出参是奖励档位。
 * 发奖、积分、现金那条链路要连库，不在这一层。
 */
class SignInRewardDayTest {

    private fun day(continuous: Int, cycle: Int, cycling: Boolean) =
        MemberSignInLogic.resolveRewardDay(continuous, cycle, cycling)

    @Test
    fun first_ever_sign_in_is_day_one_in_both_modes() {
        assertEquals(1, day(continuous = 0, cycle = 7, cycling = true))
        assertEquals(1, day(continuous = 0, cycle = 7, cycling = false))
    }

    @Test
    fun cycling_wraps_back_to_day_one_after_the_last_day() {
        // 连签 6 天 → 今天第 7 天（周期最后一天）
        assertEquals(7, day(continuous = 6, cycle = 7, cycling = true))
        // 连签 7 天 → 回到第 1 天
        assertEquals(1, day(continuous = 7, cycle = 7, cycling = true))
        assertEquals(2, day(continuous = 8, cycle = 7, cycling = true))
    }

    @Test
    fun non_cycling_caps_at_the_last_day_instead_of_stopping() {
        assertEquals(7, day(continuous = 6, cycle = 7, cycling = false))
        // 关键差异：不回绕，也不掉到 0 —— 一直按最后一天发
        assertEquals(7, day(continuous = 7, cycle = 7, cycling = false))
        assertEquals(7, day(continuous = 700, cycle = 7, cycling = false))
    }

    @Test
    fun a_single_day_cycle_never_leaves_day_one() {
        // cycle=1 是配置允许的下界；取模实现下 (n % 1) + 1 恒为 1，封顶同理
        for (n in 0..5) {
            assertEquals(1, day(continuous = n, cycle = 1, cycling = true))
            assertEquals(1, day(continuous = n, cycle = 1, cycling = false))
        }
    }

    @Test
    fun the_result_always_lands_inside_the_configured_range() {
        // 越界就查不到配置、奖励恒为 0，所以这是两种模式共同的硬约束
        for (cycle in intArrayOf(1, 7, 30, 365)) {
            for (n in intArrayOf(0, 1, cycle - 1, cycle, cycle + 1, cycle * 3, 10_000)) {
                for (cycling in booleanArrayOf(true, false)) {
                    val d = day(continuous = n, cycle = cycle, cycling = cycling)
                    assertTrue(
                        d in 1..cycle,
                        "continuous=$n cycle=$cycle cycling=$cycling produced $d",
                    )
                }
            }
        }
    }
}
