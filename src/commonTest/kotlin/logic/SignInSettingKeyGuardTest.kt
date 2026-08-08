package logic

import neton.core.http.BadRequestException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * 签到设置入口的权限边界。
 *
 * 这个入口的权限是 `member:signin:update`（运营就有），但它写的是那张所有模块共用的
 * system_settings 表。挡住非签到 key 是唯一让两者不等价的东西 —— 漏了这条，
 * 运营就能借道改支付、钱包一类的全局设置。
 */
class SignInSettingKeyGuardTest {

    private fun guard(key: String) = MemberSignInLogic.requireSignInSettingKey(key)

    @Test
    fun sign_in_keys_pass() {
        guard("member.signin.cycle_days")
        guard("member.signin.cycle_enabled")
        // 还不存在的签到设置也应放行：加了新项不该还要来改这里
        guard("member.signin.something_added_later")
    }

    @Test
    fun other_modules_settings_are_rejected() {
        for (key in listOf(
            "payment.withdraw.min_amount",
            "member.profile.nickname_required",
            "system.security.password_policy",
        )) {
            assertFailsWith<BadRequestException>("应拒绝 $key") { guard(key) }
        }
    }

    @Test
    fun near_misses_are_rejected() {
        // 少个点就成了另一个命名空间的前缀匹配，必须挡住
        assertFailsWith<BadRequestException> { guard("member.signinx.cycle_days") }
        // 前缀不在开头不算数
        assertFailsWith<BadRequestException> { guard("evil.member.signin.cycle_days") }
        assertFailsWith<BadRequestException> { guard("") }
    }

    @Test
    fun the_rejection_names_the_offending_key() {
        // 运营看到的提示要能指出是哪个 key 被拒，否则只能猜
        val e = assertFailsWith<BadRequestException> { guard("payment.fee.rate") }
        assertEquals(true, e.message?.contains("payment.fee.rate"))
    }
}
