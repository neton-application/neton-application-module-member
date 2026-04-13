package controller.admin.signin

import controller.admin.signin.dto.CreateMemberSignInConfigRequest
import controller.admin.signin.dto.UpdateMemberSignInConfigRequest
import logic.MemberSignInLogic
import model.MemberSignInConfig
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Post
import neton.core.annotations.Put
import neton.core.annotations.Delete
import neton.core.annotations.Body
import neton.core.annotations.PathVariable

@Controller("/member/sign-in/config")
class MemberSignInConfigController(
    private val memberSignInLogic: MemberSignInLogic
) {

    @Post("/create")
    suspend fun create(@Body request: CreateMemberSignInConfigRequest): Long {
        return memberSignInLogic.createConfig(
            MemberSignInConfig(
                day = request.day,
                point = request.point,
                experience = request.experience,
                status = request.status
            )
        )
    }

    @Put("/update")
    suspend fun update(@Body request: UpdateMemberSignInConfigRequest) {
        memberSignInLogic.updateConfig(
            MemberSignInConfig(
                id = request.id,
                day = request.day,
                point = request.point,
                experience = request.experience,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    suspend fun delete(@PathVariable id: Long) {
        memberSignInLogic.deleteConfig(id)
    }

    @Get("/get/{id}")
    suspend fun get(@PathVariable id: Long): MemberSignInConfig? {
        return memberSignInLogic.getConfig(id)
    }

    @Get("/list")
    suspend fun list(): List<MemberSignInConfig> {
        return memberSignInLogic.listConfigs()
    }
}
