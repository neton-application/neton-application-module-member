package controller.app.invite

import kotlinx.serialization.Serializable
import neton.validation.annotations.NotBlank
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Body
import neton.core.interfaces.Identity
import logic.MemberInviteLogic
import port.MemberAuthPolicy

/** 邀请码 app 端（MEMBER_INVITE_CODE §5.2）。 */
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

    /** 谁邀请了我（被邀请方绑定）。 */
    @Get("/my")
    suspend fun my(identity: Identity): MyBindingVO {
        val record = inviteLogic.myBinding(identity.id.toLong()) ?: return MyBindingVO(bound = false)
        return MyBindingVO(
            bound = true,
            code = record.code,
            inviterUserId = record.inviterUserId,
            autoFriendStatus = record.autoFriendStatus,
            boundAt = record.boundAt,
        )
    }

    @Serializable
    data class MyCodeVO(
        val code: String,
        val usedCount: Int,
    )

    /** 我的专属邀请码（不存在则生成）。用于分享/URL 携带邀请他人。 */
    @Get("/mine")
    suspend fun mine(identity: Identity): MyCodeVO {
        val entity = inviteLogic.getOrCreateMyCode(identity.id.toLong())
        return MyCodeVO(code = entity.code, usedCount = entity.usedCount)
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

    /** 注册后补填邀请码。 */
    @Post("/bind")
    suspend fun bind(identity: Identity, @Body request: BindRequest): BindResultVO {
        val record = inviteLogic.bind(
            userId = identity.id.toLong(),
            rawCode = request.inviteCode,
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
