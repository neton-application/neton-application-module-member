package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Min

@Serializable
data class UpdateMemberUserGroupRequest(
    @property:Min(1)
    val id: Long,
    /** null = 移出分组 */
    @property:Min(0)
    val groupId: Long? = null,
)

/** 会员档案。账号信息（昵称、手机号……）在 account 的后台接口里，前端按 userId 合并。 */
@Serializable
data class MemberProfileVO(
    val userId: Long,
    val levelId: Long? = null,
    val levelName: String? = null,
    val experience: Long = 0,
    val point: Int = 0,
    val groupId: Long? = null,
    val groupName: String? = null,
)
