package controller.app.invite

import kotlinx.serialization.Serializable
import neton.validation.annotations.NotBlank
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Body
import logic.MemberInviteLogic
import port.MemberAuthPolicy

/** 【我】→【填写邀请码】(MEMBER_INVITE_CODE §5.2)。userId 传参约定与 sign-in 接口一致。 */
@Controller("/member/invite-code")
class MemberInviteCodeController(
    private val inviteLogic: MemberInviteLogic,
) {

    @Serializable
    data class MyBindingVO(
        val bound: Boolean,
        val code: String? = null,
        val inviterUserId: Long? = null,
        val autoFriendStatus: Int = 0,
        val boundAt: Long? = null,
    )

    @Get("/my")
    suspend fun my(userId: Long): MyBindingVO {
        val record = inviteLogic.myBinding(userId) ?: return MyBindingVO(bound = false)
        return MyBindingVO(
            bound = true,
            code = record.code,
            inviterUserId = record.inviterUserId,
            autoFriendStatus = record.autoFriendStatus,
            boundAt = record.boundAt,
        )
    }

    @Serializable
    data class BindRequest(
        @property:NotBlank
        val inviteCode: String,
    )

    @Serializable
    data class BindResultVO(
        val bound: Boolean,
        val code: String,
        val inviterUserId: Long? = null,
        val autoFriendStatus: Int = 0,
    )

    @Post("/bind")
    suspend fun bind(userId: Long, @Body request: BindRequest): BindResultVO {
        val record = inviteLogic.bind(
            userId = userId,
            rawCode = request.inviteCode,
            // 补填与注册方式无关;registerMode 列在补填场景仅作记录占位。
            registerMode = MemberAuthPolicy.MODE_PHONE_SMS,
            identifierMasked = null,
        )
        return BindResultVO(
            bound = true,
            code = record.code,
            inviterUserId = record.inviterUserId,
            autoFriendStatus = record.autoFriendStatus,
        )
    }
}
