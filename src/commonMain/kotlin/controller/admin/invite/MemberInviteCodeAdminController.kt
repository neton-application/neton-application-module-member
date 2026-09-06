package controller.admin.invite

import kotlinx.serialization.Serializable
import neton.validation.annotations.Min
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Permission
import logic.MemberInviteLogic
import model.MemberInviteCode
import model.MemberInviteRecord

/**
 * 邀请码后台管理(MEMBER_INVITE_CODE §7)。
 * 路由防撞:app 侧已占 /member/invite-code/{my,bind} —— admin 列表用 /list-all、
 * 记录用 /records/{codeId}(跨 group method+pattern 全局去重铁律)。
 */
@Controller("/member/invite-code")
class MemberInviteCodeAdminController(
    private val inviteLogic: MemberInviteLogic,
) {

    @Serializable
    data class SaveInviteCodeRequest(
        val id: Long = 0,
        /** 留空 = 服务端生成 */
        val code: String = "",
        val ownerUserId: Long? = null,
        @property:Min(0)
        val maxUses: Int = 0,
        val status: Int = 1,
        val expiresAt: Long? = null,
        val remark: String? = null,
        /** 绑码后邀请人自动发送的打招呼用语;空=用全局配置兜底。 */
        val welcomeMessage: String? = null,
    )

    @Get("/list-all")
    @Permission("member:invite-code:query")
    suspend fun listAll(): List<MemberInviteCode> = inviteLogic.listAll()

    /** 编辑弹窗打开时实时拉取(不信任列表快照,避免改后回填旧值)。 */
    @Get("/detail/{id}")
    @Permission("member:invite-code:query")
    suspend fun detail(@PathVariable id: Long): MemberInviteCode? = inviteLogic.adminGet(id)

    @Post("/create")
    @Permission("member:invite-code:create")
    suspend fun create(@Body request: SaveInviteCodeRequest): MemberInviteCode =
        inviteLogic.adminCreate(
            MemberInviteCode(
                code = request.code,
                ownerUserId = request.ownerUserId,
                maxUses = request.maxUses,
                status = request.status,
                expiresAt = request.expiresAt,
                remark = request.remark,
                welcomeMessage = request.welcomeMessage,
            ),
        )

    @Put("/update")
    @Permission("member:invite-code:update")
    suspend fun update(@Body request: SaveInviteCodeRequest) {
        inviteLogic.adminUpdate(
            MemberInviteCode(
                id = request.id,
                code = request.code,
                ownerUserId = request.ownerUserId,
                maxUses = request.maxUses,
                status = request.status,
                expiresAt = request.expiresAt,
                remark = request.remark,
                welcomeMessage = request.welcomeMessage,
            ),
        )
    }

    @Delete("/delete/{id}")
    @Permission("member:invite-code:delete")
    suspend fun delete(@PathVariable id: Long) {
        inviteLogic.adminDelete(id)
    }

    /** 某邀请码的全部邀请记录(详情抽屉)。 */
    @Get("/records/{codeId}")
    @Permission("member:invite-code:query")
    suspend fun records(@PathVariable codeId: Long): List<MemberInviteRecord> =
        inviteLogic.recordsOfCode(codeId)

    /** 某用户的邀请来源(用户列表「邀请来源」列)。 */
    @Get("/record-of-invitee/{userId}")
    @Permission("member:invite-code:query")
    suspend fun recordOfInvitee(@PathVariable userId: Long): MemberInviteRecord? =
        inviteLogic.recordOfInvitee(userId)

    /** 补偿:FAILED 记录重试自动加好友。 */
    @Post("/retry-auto-friend/{recordId}")
    @Permission("member:invite-code:update")
    suspend fun retryAutoFriend(@PathVariable recordId: Long) {
        inviteLogic.retryAutoFriend(recordId)
    }

    /**
     * 补偿:重放邀请奖励回调。下游发奖失败(额度库抖动、配置刚修好)时用它补发。
     * 200 只代表事件已重放,**不代表已到账** —— 各回调按自己的 ref 幂等,
     * 到账与否要去下游账本查(见 [logic.MemberInviteLogic.retryInviteReward] 的说明)。
     */
    @Post("/retry-invite-reward/{recordId}")
    @Permission("member:invite-code:update")
    suspend fun retryInviteReward(@PathVariable recordId: Long) {
        inviteLogic.retryInviteReward(recordId)
    }
}
