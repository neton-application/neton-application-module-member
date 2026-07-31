package controller.admin.member

import controller.admin.member.dto.FillBotNicknamesResponse
import controller.admin.member.dto.MemberVO
import controller.admin.member.dto.UpdateMemberRequest
import controller.admin.member.dto.UpdateMemberUserLevelRequest
import controller.admin.member.dto.UpdateMemberUserPasswordRequest
import controller.admin.member.dto.UpdateMemberUserPointRequest
import dto.PageResponse
import logic.MemberLogic
import logic.NicknameGenerator
import logic.MemberGeoLocationService
import model.Member
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Body
import neton.core.annotations.PathVariable
import neton.core.annotations.Query

@Controller("/member/user")
class MemberUserController(
    private val memberLogic: MemberLogic,
    private val nicknameGenerator: NicknameGenerator,
) {
    private val geoLocation = MemberGeoLocationService.fromConfig()

    @Put("/update")
    @Permission("member:user:update")
    suspend fun update(@Body request: UpdateMemberRequest) {
        val existing = memberLogic.get(request.id)
            ?: throw IllegalArgumentException("Member not found: ${request.id}")
        memberLogic.update(
            existing.copy(
                mobile = request.mobile,
                nickname = request.nickname,
                avatar = request.avatar,
                status = request.status,
                levelId = request.levelId,
                groupId = request.groupId
            )
        )
    }

    @Put("/update-level")
    @Permission("member:user:update")
    suspend fun updateLevel(@Body req: UpdateMemberUserLevelRequest) {
        memberLogic.updateLevel(req.id, req.levelId, 0, null)
    }

    @Put("/update-point")
    @Permission("member:user:update")
    suspend fun updatePoint(@Body req: UpdateMemberUserPointRequest) {
        memberLogic.updatePoint(req.id, req.point, 1, "Admin adjust", null)
    }

    /**
     * 管理员重设会员登录密码（客服/找回场景）。不需要旧密码，也不返回任何密码信息；
     * 明文经 PasswordHasher 加盐哈希后落库，改完会员即可用新密码登录。
     * 单独权限点 `member:user:update-password`——重设密码等于接管账号，不能和普通编辑同权。
     */
    @Put("/update-password")
    @Permission("member:user:update-password")
    suspend fun updatePassword(@Body req: UpdateMemberUserPasswordRequest) {
        // @Size 之外再显式挡一次：空串/纯空格在部分校验实现下会漏过（历史坑）。
        val password = req.password.trim()
        if (password.length < 8) {
            throw IllegalArgumentException("Password must be at least 8 characters")
        }
        memberLogic.updatePassword(req.id, password)
    }

    @Get("/get/{id}")
    @Permission("member:user:query")
    suspend fun get(@PathVariable id: Long): MemberVO? {
        return memberLogic.get(id)?.toAdminVO()
    }

    @Get("/page")
    @Permission("member:user:query")
    suspend fun page(
        @Query pageNo: Int = 1,
        @Query pageSize: Int = 10,
        @Query nickname: String? = null,
        @Query mobile: String? = null,
        @Query uid: Long? = null,
        @Query username: String? = null,
        @Query status: Int? = null,
        @Query levelId: Long? = null,
        @Query groupId: Long? = null,
        // 默认隐藏陪玩机器人；admin 需要时 ?includeRobot=true。
        @Query includeRobot: Boolean = false
    ): PageResponse<MemberVO> = memberLogic.page(
        pageNo,
        pageSize,
        nickname,
        mobile,
        uid,
        username,
        status,
        levelId,
        groupId,
        includeRobot,
    ).let { page ->
        PageResponse(
            list = page.list.map { it.toAdminVO() },
            total = page.total,
            page = page.page,
            size = page.size,
            totalPages = page.totalPages,
        )
    }

    private fun Member.toAdminVO() = MemberVO(
        id = id,
        identityProvider = identityProvider,
        username = username,
        usernameUpdatedAt = usernameUpdatedAt,
        mobile = mobile,
        nickname = nickname,
        avatar = avatar,
        gender = gender,
        bio = bio,
        birthday = birthday,
        isRobot = isRobot,
        status = status,
        levelId = levelId,
        experience = experience,
        point = point,
        groupId = groupId,
        registerIp = registerIp,
        loginIp = loginIp,
        loginRegion = geoLocation.resolve(loginIp),
        loginDate = loginDate,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    /** 扫所有 is_robot=1 且 nickname 为 null/空 的陪玩账号，用 [NicknameGenerator] 词库随机补昵称。
     *
     *  - 词库未初始化 / 任一边空 → generator 返 null → 该条 skipped (pool_empty)
     *  - 已有非空 nickname 的 bot 不动 (admin 想重抽得删除原 nickname 再调)
     *  - 每条单独尝试,某条失败不阻断其他 (errors 计数)
     *
     *  权限走 member:user:update (跟改昵称同一级别)。
     */
    @Post("/fill-bot-nicknames")
    @Permission("member:user:update")
    suspend fun fillBotNicknames(
        /** true=只补 nickname='' 的 (增量); false (默认)=重抽所有 is_robot=1 (强制刷新). */
        @Query onlyEmpty: Boolean = false,
    ): FillBotNicknamesResponse {
        val result = memberLogic.fillBotNicknames(nicknameGenerator, onlyEmpty = onlyEmpty)
        return FillBotNicknamesResponse(
            scanned = result.scanned,
            filled = result.filled,
            skippedPoolEmpty = result.skippedPoolEmpty,
            errors = result.errors,
        )
    }
}
