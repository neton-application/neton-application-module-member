package controller.admin.signin

import controller.admin.signin.dto.MemberSignInSettingVO
import controller.admin.signin.dto.UpdateMemberSignInSettingRequest
import logic.MemberSignInLogic
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Post

/**
 * 签到全局设置的运营入口。
 *
 * 与系统设置接口读写同一张表，区别只在可见范围与权限：那边是超管看全部，
 * 这边只开放 `member.signin.*` 给管签到的人。前缀限制在 [MemberSignInLogic] 里强制，
 * 不在这一层 —— 换个 controller 绕过去就没意义了。
 */
@Controller("/member/sign-in/setting")
class MemberSignInSettingController(
    private val memberSignInLogic: MemberSignInLogic
) {

    @Get("/list")
    @Permission("member:signin:query")
    suspend fun list(): List<MemberSignInSettingVO> = memberSignInLogic.listSignInSettings()

    /** 值不合法由定义本身拒掉，错误停在写入时，而不是等读取时静默退回默认。 */
    @Post("/update")
    @Permission("member:signin:update")
    suspend fun update(@Body request: UpdateMemberSignInSettingRequest) {
        memberSignInLogic.updateSignInSetting(request.key, request.value)
    }
}
