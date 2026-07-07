package controller.admin.signin

import controller.admin.signin.dto.CreateMemberSignInConfigRequest
import controller.admin.signin.dto.UpdateMemberSignInConfigRequest
import logic.MemberSignInLogic
import model.MemberSignInConfig
import neton.core.annotations.Controller
import neton.core.annotations.Get
import neton.core.annotations.Permission
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
    @Permission("member:signin:create")
    suspend fun create(@Body request: CreateMemberSignInConfigRequest): Long {
        return memberSignInLogic.createConfig(
            MemberSignInConfig(
                day = request.day,
                point = request.point,
                experience = request.experience,
                cashAmount = request.cashAmount,
                status = request.status
            )
        )
    }

    @Put("/update")
    @Permission("member:signin:update")
    suspend fun update(@Body request: UpdateMemberSignInConfigRequest) {
        memberSignInLogic.updateConfig(
            MemberSignInConfig(
                id = request.id,
                day = request.day,
                point = request.point,
                experience = request.experience,
                cashAmount = request.cashAmount,
                status = request.status
            )
        )
    }

    @Delete("/delete/{id}")
    @Permission("member:signin:delete")
    suspend fun delete(@PathVariable id: Long) {
        memberSignInLogic.deleteConfig(id)
    }

    @Get("/get/{id}")
    @Permission("member:signin:query")
    suspend fun get(@PathVariable id: Long): MemberSignInConfig? {
        return memberSignInLogic.getConfig(id)
    }

    // 路由跨 group 全局去重：app 侧已有 GET /member/sign-in/config/list（客户端在用），
    // admin 侧必须换 pattern（先例：admin 牌谱 /full-replay）。
    @Get("/list-all")
    @Permission("member:signin:list")
    suspend fun list(): List<MemberSignInConfig> {
        return memberSignInLogic.listConfigs()
    }
}
