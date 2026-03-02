package controller.app.social

import controller.admin.social.dto.SocialBindRequest
import controller.admin.social.dto.SocialUserVO
import logic.SocialUserLogic
import neton.core.annotations.*
import neton.core.interfaces.Identity

@Controller("/social-user")
class SocialUserController(
    private val socialUserLogic: SocialUserLogic
) {

    @Get("/list")
    suspend fun list(identity: Identity): List<SocialUserVO> {
        val userId = identity.id.toLong()
        return socialUserLogic.listByUser(userId, userType = 2)  // 2 = member
    }

    @Post("/bind")
    suspend fun bind(identity: Identity, @Body request: SocialBindRequest): SocialUserVO {
        val userId = identity.id.toLong()
        return socialUserLogic.bind(
            userId = userId,
            userType = 2,  // member
            socialType = request.socialType,
            code = request.code,
            redirectUri = request.redirectUri ?: ""
        )
    }

    @Delete("/unbind")
    suspend fun unbind(identity: Identity, @Query socialType: String) {
        val userId = identity.id.toLong()
        socialUserLogic.unbind(userId, userType = 2, socialType = socialType)
    }
}
