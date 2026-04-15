package controller.app.auth

import logic.MemberAuthLogic
import controller.app.auth.dto.MemberLoginRequest
import controller.app.auth.dto.MemberLoginResponse
import controller.admin.auth.dto.SocialLoginRequest
import controller.admin.auth.dto.SocialRedirectVO
import kotlinx.serialization.Serializable
import neton.core.annotations.*
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Pattern
import neton.validation.annotations.Size

@Serializable
data class ValidateSmsCodeRequest(
    @property:NotBlank
    @property:Pattern(regex = "^1\\d{10}$", message = "mobile format is invalid")
    val mobile: String,

    @property:NotBlank
    @property:Size(min = 4, max = 8)
    val code: String
)

@Serializable
data class MemberRefreshTokenRequest(
    @property:NotBlank
    @property:Size(min = 32, max = 4096)
    val refreshToken: String
)

@Serializable
data class MemberSendSmsCodeRequest(
    @property:NotBlank
    @property:Pattern(regex = "^1\\d{10}$", message = "mobile format is invalid")
    val mobile: String
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
    @RateLimit(windowSeconds = 300, maxRequests = 10, scope = RateLimitScope.IP, message = "Login attempts exceeded, please try again later")
    suspend fun login(@Body request: MemberLoginRequest): MemberLoginResponse {
        return memberAuthLogic.login(request)
    }

    @Post("/sms-login")
    @AllowAnonymous
    @RateLimit(windowSeconds = 60, maxRequests = 5, scope = RateLimitScope.IP, message = "SMS login attempts exceeded, please try again later")
    suspend fun smsLogin(@Body request: MemberLoginRequest): MemberLoginResponse {
        return memberAuthLogic.smsLogin(request)
    }

    @Post("/logout")
    suspend fun logout(userId: Long) {
        memberAuthLogic.logout(userId)
    }

    @Post("/refresh-token")
    @AllowAnonymous
    suspend fun refreshToken(@Body request: MemberRefreshTokenRequest): MemberLoginResponse {
        return memberAuthLogic.refreshToken(request.refreshToken)
    }

    @Post("/send-sms-code")
    @AllowAnonymous
    @RateLimit(windowSeconds = 60, maxRequests = 5, scope = RateLimitScope.IP, message = "SMS code sending limit exceeded, please try again later")
    suspend fun sendSmsCode(@Body request: MemberSendSmsCodeRequest) {
        memberAuthLogic.sendSmsCode(request.mobile)
    }

    @Post("/validate-sms-code")
    @AllowAnonymous
    @RateLimit(windowSeconds = 60, maxRequests = 5, scope = RateLimitScope.IP, message = "Too many verification attempts, please try again later")
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
    @RateLimit(windowSeconds = 300, maxRequests = 10, scope = RateLimitScope.IP, message = "Social login attempts exceeded, please try again later")
    suspend fun socialLogin(@Body request: SocialLoginRequest): MemberLoginResponse {
        return memberAuthLogic.socialLogin(request.socialType, request.code, request.redirectUri ?: "")
    }
}
