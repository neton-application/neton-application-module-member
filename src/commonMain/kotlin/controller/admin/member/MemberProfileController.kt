package controller.admin.member

import controller.admin.member.dto.MemberProfileVO
import controller.admin.member.dto.UpdateMemberUserGroupRequest
import controller.admin.member.dto.UpdateMemberUserLevelRequest
import controller.admin.member.dto.UpdateMemberUserPointRequest
import logic.MemberGroupLogic
import logic.MemberLevelLogic
import logic.MemberLogic
import model.MemberProfile
import neton.core.annotations.Body
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.PathVariable
import neton.core.annotations.Permission
import neton.core.annotations.Put
import neton.core.annotations.Query

/**
 * 会员档案的后台接口：等级、积分、分组。
 *
 * 账号本身（昵称、手机号、状态、密码……）在 account 的 `/account/user/...` 接口里；后台页面按 userId
 * 把两边合并展示。档案相关的写操作单独一个权限点 `member:profile:update`，与账号修改分开 ——
 * 能改人积分的运营不一定该能改人手机号。
 */
@Controller("/member/user")
class MemberProfileController(
    private val memberLogic: MemberLogic,
    private val memberLevelLogic: MemberLevelLogic,
    private val memberGroupLogic: MemberGroupLogic,
) {

    @Get("/profile/{id}")
    @Permission("member:profile:query")
    suspend fun profile(@PathVariable id: Long): MemberProfileVO =
        toVOs(listOf(memberLogic.profile(id))).single()

    /** 批量取档案，供后台账号列表一页一次合并（逗号分隔，最多 200 个）。 */
    @Get("/profiles")
    @Permission("member:profile:query")
    suspend fun profiles(@Query ids: String? = null): List<MemberProfileVO> {
        val userIds = ids.orEmpty().split(',').mapNotNull { it.trim().toLongOrNull() }.distinct().take(200)
        return toVOs(memberLogic.profiles(userIds).values.toList())
    }

    @Put("/update-level")
    @Permission("member:profile:update")
    suspend fun updateLevel(@Body req: UpdateMemberUserLevelRequest) {
        memberLogic.updateLevel(req.id, req.levelId, 0, null)
    }

    @Put("/update-point")
    @Permission("member:profile:update")
    suspend fun updatePoint(@Body req: UpdateMemberUserPointRequest) {
        memberLogic.changePoint(req.id, req.point, 1, "Admin adjust", null)
    }

    @Put("/update-group")
    @Permission("member:profile:update")
    suspend fun updateGroup(@Body req: UpdateMemberUserGroupRequest) {
        memberLogic.updateGroup(req.id, req.groupId)
    }

    // 等级 / 分组名整批一次解析：两张都是几十行的小字典，整表拉下来比逐行查便宜，也避免 N+1。
    private suspend fun toVOs(profiles: List<MemberProfile>): List<MemberProfileVO> {
        val levelNames = memberLevelLogic.list().associate { it.id to it.name }
        val groupNames = memberGroupLogic.listAll().associate { it.id to it.name }
        return profiles.map {
            MemberProfileVO(
                userId = it.userId,
                levelId = it.levelId,
                levelName = it.levelId?.let(levelNames::get),
                experience = it.experience,
                point = it.point,
                groupId = it.groupId,
                groupName = it.groupId?.let(groupNames::get),
            )
        }
    }
}
