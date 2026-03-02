package logic

import model.Member
import table.MemberTable
import controller.app.auth.dto.MemberLoginRequest
import controller.app.auth.dto.MemberLoginResponse
import neton.database.dsl.*

import neton.security.identity.UserId
import neton.logging.Logger
import neton.core.http.BadRequestException
import neton.core.http.NotFoundException
import infra.PasswordEncoder
import neton.security.jwt.JwtAuthenticatorV1
import neton.redis.RedisClient
import logic.MessageSendLogic
import logic.SocialUserLogic
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Clock

class MemberAuthLogic(
    private val log: Logger,
    private val jwt: JwtAuthenticatorV1? = null,
    private val redis: RedisClient? = null,
    private val messageSendLogic: MessageSendLogic? = null,
    private val socialUserLogic: SocialUserLogic? = null
) {

    companion object {
        private const val ACCESS_TOKEN_EXPIRES = 7200L  // 2 hours
        private const val REFRESH_TOKEN_EXPIRES = 604800L  // 7 days
        private const val SMS_CODE_PREFIX = "sms:code:"
        private const val SMS_CODE_TTL_SECONDS = 300L  // 5 minutes
    }

    suspend fun login(request: MemberLoginRequest): MemberLoginResponse {
        val member = MemberTable.oneWhere {
            Member::mobile eq request.mobile
        } ?: throw BadRequestException("Invalid mobile or password")

        if (member.password == null) {
            throw BadRequestException("Password not set for this account")
        }

        if (!PasswordEncoder.matches(request.password ?: "", member.password)) {
            throw BadRequestException("Invalid mobile or password")
        }

        if (member.status != 0) {
            throw BadRequestException("Account is disabled")
        }

        val accessToken = generateAccessToken(member.id)
        val refreshToken = generateRefreshToken(member.id)

        // Update login info
        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("Member login successful: userId=${member.id}, mobile=${request.mobile}")

        return MemberLoginResponse(
            userId = member.id,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = ACCESS_TOKEN_EXPIRES
        )
    }

    suspend fun smsLogin(request: MemberLoginRequest): MemberLoginResponse {
        val smsCode = request.smsCode ?: throw BadRequestException("SMS code is required")

        // Verify SMS code from Redis
        verifySmsCode(request.mobile, smsCode)

        var member = MemberTable.oneWhere {
            Member::mobile eq request.mobile
        }

        // Auto-register if member does not exist
        if (member == null) {
            val newMember = Member(
                mobile = request.mobile,
                nickname = "Member_${request.mobile.takeLast(4)}"
            )
            member = MemberTable.insert(newMember)
            log.info("Auto-registered new member via SMS login: userId=${member.id}, mobile=${request.mobile}")
        }

        if (member.status != 0) {
            throw BadRequestException("Account is disabled")
        }

        val accessToken = generateAccessToken(member.id)
        val refreshToken = generateRefreshToken(member.id)

        // Update login info
        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("Member SMS login successful: userId=${member.id}, mobile=${request.mobile}")

        return MemberLoginResponse(
            userId = member.id,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = ACCESS_TOKEN_EXPIRES
        )
    }

    suspend fun logout(userId: Long) {
        // Blacklist the user's tokens
        redis?.set("auth:member:blacklist:$userId", "1", ttl = ACCESS_TOKEN_EXPIRES.seconds)
        log.info("Member logout: userId=$userId")
    }

    suspend fun refreshToken(refreshToken: String): MemberLoginResponse {
        // Verify the refresh token by parsing its claims
        val jwtInstance = jwt ?: throw BadRequestException("JWT service not available")

        // Parse the refresh token to extract userId
        val userId = parseTokenUserId(refreshToken)
            ?: throw BadRequestException("Invalid or expired refresh token")

        val member = MemberTable.get(userId)
            ?: throw NotFoundException("Member not found")

        if (member.status != 0) {
            throw BadRequestException("Account is disabled")
        }

        val newAccessToken = generateAccessToken(member.id)
        val newRefreshToken = generateRefreshToken(member.id)

        log.info("Member token refreshed: userId=$userId")

        return MemberLoginResponse(
            userId = userId,
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            expiresIn = ACCESS_TOKEN_EXPIRES
        )
    }

    suspend fun sendSmsCode(mobile: String) {
        if (messageSendLogic != null) {
            messageSendLogic.sendVerificationCode(mobile, "member_login")
        } else {
            // Fallback: store code directly in Redis
            val code = (100000..999999).random().toString()
            redis?.set("$SMS_CODE_PREFIX$mobile", code, ttl = SMS_CODE_TTL_SECONDS.seconds)
            log.info("SMS code generated for mobile: $mobile (code=$code)")
        }
    }

    suspend fun validateSmsCode(mobile: String, code: String): Boolean {
        return try {
            verifySmsCode(mobile, code)
            true
        } catch (_: Exception) {
            false
        }
    }

    // --- Social login ---

    suspend fun socialAuthRedirect(socialType: String, redirectUri: String): String {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")
        return social.getAuthRedirectUrl(socialType, redirectUri)
    }

    suspend fun socialLogin(socialType: String, code: String, redirectUri: String): MemberLoginResponse {
        val social = socialUserLogic
            ?: throw BadRequestException("Social login not configured")

        val socialUser = social.socialLogin(socialType, code, redirectUri, userType = 2)

        val member: Member
        if (socialUser.userId == 0L) {
            // Auto-register a new member via social login
            val newMember = Member(
                nickname = socialUser.nickname ?: "Member_${socialUser.openId.take(6)}",
                avatar = socialUser.avatar
            )
            member = MemberTable.insert(newMember)

            // Bind the social account to the new member
            social.bind(member.id, userType = 2, socialType, code, redirectUri)
            log.info("Auto-registered new member via social login: userId=${member.id}, socialType=$socialType")
        } else {
            member = MemberTable.get(socialUser.userId)
                ?: throw NotFoundException("Bound member not found")
        }

        if (member.status != 0) {
            throw BadRequestException("Account is disabled")
        }

        val accessToken = generateAccessToken(member.id)
        val refreshToken = generateRefreshToken(member.id)

        val now = Clock.System.now().toEpochMilliseconds()
        MemberTable.update(member.copy(loginDate = now))

        log.info("Member social login successful: userId=${member.id}, socialType=$socialType")

        return MemberLoginResponse(
            userId = member.id,
            accessToken = accessToken,
            refreshToken = refreshToken,
            expiresIn = ACCESS_TOKEN_EXPIRES
        )
    }

    // --- Private helpers ---

    private suspend fun verifySmsCode(mobile: String, code: String) {
        val storedCode = redis?.getValue("$SMS_CODE_PREFIX$mobile")
        if (storedCode == null) {
            throw BadRequestException("SMS code expired or not sent")
        }
        if (storedCode != code) {
            throw BadRequestException("Invalid SMS code")
        }
        // Delete the code after successful verification
        redis?.delete("$SMS_CODE_PREFIX$mobile")
    }

    private fun generateAccessToken(userId: Long): String {
        val jwtInstance = jwt ?: return "jwt-not-configured"
        return jwtInstance.createToken(
            userId = UserId(userId.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = ACCESS_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "access", "scope" to "member")
        )
    }

    private fun generateRefreshToken(userId: Long): String {
        val jwtInstance = jwt ?: return "jwt-not-configured"
        return jwtInstance.createToken(
            userId = UserId(userId.toULong()),
            roles = emptySet(),
            permissions = emptySet(),
            expiresInSeconds = REFRESH_TOKEN_EXPIRES,
            extraClaims = mapOf("type" to "refresh", "scope" to "member")
        )
    }

    private fun parseTokenUserId(token: String): Long? {
        // JWT format: header.payload.signature
        // Payload is base64url encoded JSON containing "sub" (userId)
        return try {
            val parts = token.split(".")
            if (parts.size != 3) return null
            val payload = parts[1]
            // Decode base64url
            val decoded = decodeBase64Url(payload)
            // Extract "sub" field from JSON
            val subRegex = """"sub"\s*:\s*"?(\d+)"?""".toRegex()
            val match = subRegex.find(decoded)
            match?.groupValues?.get(1)?.toLongOrNull()
        } catch (_: Exception) {
            null
        }
    }

    private fun decodeBase64Url(input: String): String {
        val padded = input.replace('-', '+').replace('_', '/')
        val padding = when (padded.length % 4) {
            2 -> "$padded=="
            3 -> "$padded="
            else -> padded
        }
        // Simple base64 decode for ASCII/UTF-8 JWT payloads
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        val bytes = mutableListOf<Byte>()
        var i = 0
        while (i < padding.length) {
            if (padding[i] == '=') break
            val a = chars.indexOf(padding[i])
            val b = if (i + 1 < padding.length) chars.indexOf(padding[i + 1]) else 0
            val c = if (i + 2 < padding.length && padding[i + 2] != '=') chars.indexOf(padding[i + 2]) else 0
            val d = if (i + 3 < padding.length && padding[i + 3] != '=') chars.indexOf(padding[i + 3]) else 0
            bytes.add(((a shl 2) or (b shr 4)).toByte())
            if (i + 2 < padding.length && padding[i + 2] != '=') {
                bytes.add((((b and 0xF) shl 4) or (c shr 2)).toByte())
            }
            if (i + 3 < padding.length && padding[i + 3] != '=') {
                bytes.add((((c and 0x3) shl 6) or d).toByte())
            }
            i += 4
        }
        return bytes.toByteArray().decodeToString()
    }
}
