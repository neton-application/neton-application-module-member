package logic

import contract.testing.ErrorMessageGuard
import kotlin.test.Test

/** member 的业务错误 message 必须是 [MemberErrorCodes] 里的机器码（spec application/member-errors）。 */
class ErrorMessageGuardTest {
    @Test
    fun error_messages_are_machine_codes() {
        ErrorMessageGuard.assertModuleClean(
            allowlist = setOf(
                // MemberErrorCodes.field(path, code) 本身：把调用方传入的目录常量放进 message 与 code，
                // 调用点都传 MemberErrorCodes.* 常量（被本守卫逐个检查）。
                "kotlin/logic/MemberErrorCodes.kt#ValidationError#code",
            ),
        )
    }
}
