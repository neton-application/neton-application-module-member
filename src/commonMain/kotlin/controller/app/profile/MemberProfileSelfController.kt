package controller.app.profile

import kotlinx.serialization.Serializable
import logic.MemberLevelLogic
import logic.MemberLogic
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.interfaces.Identity

@Serializable
data class MyMemberProfileVO(
    val levelId: Long? = null,
    val levelName: String? = null,
    /** 等级序号（1、2、3……），客户端用它渲染「Lv.N」。没有等级时为 null。 */
    val level: Int? = null,
    val experience: Long = 0,
    val point: Int = 0,
)

/**
 * 当前用户的会员档案（等级、经验、积分）。
 *
 * 拆分前这些字段混在账号的 `/member/user/get` 里；那个接口现在只返回账号资料，成长数据从这里取。
 */
@Controller("/member/profile")
class MemberProfileSelfController(
    private val memberLogic: MemberLogic,
    private val memberLevelLogic: MemberLevelLogic,
) {
    @Get("/get")
    suspend fun mine(identity: Identity): MyMemberProfileVO {
        val profile = memberLogic.profile(identity.id.toLong())
        val level = profile.levelId?.let { memberLevelLogic.get(it) }
        return MyMemberProfileVO(
            levelId = profile.levelId,
            levelName = level?.name,
            level = level?.level,
            experience = profile.experience,
            point = profile.point,
        )
    }
}
