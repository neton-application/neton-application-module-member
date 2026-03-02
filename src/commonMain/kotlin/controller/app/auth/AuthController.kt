package controller.app.auth

import logic.MemberAuthLogic
import controller.app.auth.dto.MemberLoginRequest
import controller.app.auth.dto.MemberLoginResponse
import controller.admin.auth.dto.SocialLoginRequest
import controller.admin.auth.dto.SocialRedirectVO
import kotlinx.serialization.Serializable
import neton.core.annotations.*

@Serializable
data class ValidateSmsCodeRequest(
    val mobile: String,
    val code: String
)

@Serializable
data class ValidateSmsCodeResponse(
    val valid: Boolean
)

@Controller("/auth")
class AuthController(
    private val memberAuthLogic: MemberAuthLogic
) {

    @Post("/login")
    @AllowAnonymous
    suspend fun login(@Body request: MemberLoginRequest): MemberLoginResponse {
        return memberAuthLogic.login(request)
    }

    @Post("/sms-login")
    @AllowAnonymous
    suspend fun smsLogin(@Body request: MemberLoginRequest): MemberLoginResponse {
        return memberAuthLogic.smsLogin(request)
    }

    @Post("/logout")
    suspend fun logout(userId: Long) {
        memberAuthLogic.logout(userId)
    }

    @Post("/refresh-token")
    @AllowAnonymous
    suspend fun refreshToken(@Body request: Map<String, String>): MemberLoginResponse {
        val token = request["refreshToken"] ?: throw neton.core.http.BadRequestException("refreshToken is required")
        return memberAuthLogic.refreshToken(token)
    }

    @Post("/send-sms-code")
    @AllowAnonymous
    suspend fun sendSmsCode(@Body request: Map<String, String>) {
        val mobile = request["mobile"] ?: throw neton.core.http.BadRequestException("mobile is required")
        memberAuthLogic.sendSmsCode(mobile)
    }

    @Post("/validate-sms-code")
    @AllowAnonymous
    suspend fun validateSmsCode(@Body request: ValidateSmsCodeRequest): ValidateSmsCodeResponse {
        val valid = memberAuthLogic.validateSmsCode(request.mobile, request.code)
        return ValidateSmsCodeResponse(valid = valid)
    }

    @Get("/social-auth-redirect")
    @AllowAnonymous
    suspend fun socialAuthRedirect(@Query type: String, @Query redirectUri: String? = null): SocialRedirectVO {
        val url = memberAuthLogic.socialAuthRedirect(type, redirectUri ?: "")
        return SocialRedirectVO(url = url)
    }

    @Post("/social-login")
    @AllowAnonymous
    suspend fun socialLogin(@Body request: SocialLoginRequest): MemberLoginResponse {
        return memberAuthLogic.socialLogin(request.socialType, request.code, request.redirectUri ?: "")
    }
}
