package logic

import com.netonstream.privchat.application.module.privchat.hook.HookBus
import com.netonstream.privchat.application.module.privchat.hook.hooks.MemberProfileChangedHook
import com.netonstream.privchat.application.module.privchat.hook.hooks.MemberProfileSnapshot
import kotlin.time.Clock
import model.Member
import neton.core.http.BadRequestException
import neton.core.http.HttpException
import neton.core.http.NetonErrorCode
import neton.core.http.NotFoundException
import neton.logging.Logger
import logic.AppFileLogic

/**
 * Profile 字段变更的统一入口（spec MODULE_MEMBER_PROFILE_SPEC §4）。
 *
 * 调用约定：
 * - 每次成功写库后 publish [MemberProfileChangedHook]，`changedFields` 只列**本次**改动的字段
 * - username 的格式校验、保留词、30 天频控、UNIQUE 兜底全部在本类完成；server 端不重做
 * - mobile / password 不走本类（语义不同：要 SMS / 旧密码 + `@FreshAuth`），保留在 [MemberAuthLogic] / [MemberUserController]
 */
class MemberProfileLogic(
    private val log: Logger,
    private val memberLogic: MemberLogic,
    private val appFileLogic: AppFileLogic,
    private val hookBus: HookBus? = null,
) {

    suspend fun updateNickname(uid: Long, nickname: String): Member {
        val trimmed = nickname.trim()
        if (trimmed.length !in 2..32) {
            throw BadRequestException("INVALID_NICKNAME_LENGTH")
        }
        val member = requireMember(uid)
        val updated = member.copy(nickname = trimmed)
        memberLogic.update(updated)
        publishProfileChanged(updated, setOf("nickname"))
        return updated
    }

    /**
     * 头像更新（spec MODULE_MEMBER_PROFILE_SPEC §4.3）。
     *
     * 入参是 application 唯一上传入口（POST /app-api/infra/file/upload）返回的 fileId，
     * 不是任意 URL。logic 层做完整的归属 / 业务类型 / status 校验后，把 file.url
     * 写到 member.avatar。
     *
     * 替代旧方案"avatarUrl + URL 白名单前缀匹配" —— 数据库 lookup + 归属检查
     * 比 URL 字符串前缀更可靠。
     */
    suspend fun updateAvatar(uid: Long, fileId: String): Member {
        val trimmed = fileId.trim()
        if (trimmed.isEmpty()) {
            throw BadRequestException("INVALID_AVATAR_FILE_ID")
        }
        val record = appFileLogic.getByFileId(trimmed)
            ?: throw BadRequestException("AVATAR_FILE_NOT_FOUND: $trimmed")

        if (record.status != 1) {
            throw BadRequestException("AVATAR_FILE_INACTIVE: $trimmed")
        }
        if (record.ownerUid != uid) {
            // 不暴露归属信息——直接 not-found 语义，避免泄露 fileId 存在性
            throw BadRequestException("AVATAR_FILE_NOT_FOUND: $trimmed")
        }
        if (record.businessType != "member_avatar") {
            throw BadRequestException(
                "INVALID_AVATAR_BUSINESS_TYPE: expected member_avatar, got ${record.businessType}",
            )
        }
        val avatarUrl = record.url
            ?: throw BadRequestException("AVATAR_FILE_NO_URL: $trimmed")

        val member = requireMember(uid)
        val updated = member.copy(avatar = avatarUrl)
        memberLogic.update(updated)
        publishProfileChanged(updated, setOf("avatar"))
        return updated
    }

    suspend fun updateUsername(uid: Long, username: String): UsernameUpdateResult {
        val normalized = username.trim().lowercase()
        if (!USERNAME_REGEX.matches(normalized)) {
            throw BadRequestException("INVALID_USERNAME_FORMAT")
        }
        if (normalized in USERNAME_RESERVED) {
            throw BadRequestException("USERNAME_RESERVED")
        }

        val member = requireMember(uid)
        val now = nowMillis()
        member.usernameUpdatedAt?.let { last ->
            val elapsed = now - last
            if (elapsed in 0 until USERNAME_RATE_LIMIT_MS) {
                val retryAfterSecs = (USERNAME_RATE_LIMIT_MS - elapsed) / 1000
                throw HttpException(
                    code = NetonErrorCode.RATE_LIMIT_EXCEEDED,
                    message = "USERNAME_RATE_LIMITED retryAfterSecs=$retryAfterSecs",
                )
            }
        }
        if (member.username == normalized) {
            // 同名则视作 no-op，但仍要刷 usernameUpdatedAt 是否合理？不应：避免用户用 set-same 偷过 30 天闸。
            return UsernameUpdateResult(
                member = member,
                nextChangeAvailableAt = (member.usernameUpdatedAt ?: 0L) + USERNAME_RATE_LIMIT_MS,
            )
        }

        val updated = member.copy(
            username = normalized,
            usernameUpdatedAt = now,
        )
        try {
            memberLogic.update(updated)
        } catch (e: Throwable) {
            // UNIQUE 约束冲突在不同 driver 下错误形态不一；按 message 关键字粗判，
            // 兜底归类为 USERNAME_TAKEN（DB 是真正守门）。
            if (isUniqueViolation(e, target = "username")) {
                throw HttpException(
                    code = NetonErrorCode.OPERATION_CONFLICT,
                    message = "USERNAME_TAKEN",
                )
            }
            throw e
        }
        publishProfileChanged(updated, setOf("username"))
        log.info("member.username changed uid=$uid old=${member.username} new=$normalized")
        return UsernameUpdateResult(
            member = updated,
            nextChangeAvailableAt = now + USERNAME_RATE_LIMIT_MS,
        )
    }

    suspend fun updateBio(uid: Long, bio: String?): Member {
        val normalized = bio?.takeIf { it.isNotEmpty() }
        if (normalized != null && normalized.length > 200) {
            throw BadRequestException("INVALID_BIO_LENGTH")
        }
        val member = requireMember(uid)
        val updated = member.copy(bio = normalized)
        memberLogic.update(updated)
        publishProfileChanged(updated, setOf("bio"))
        return updated
    }

    suspend fun updateGender(uid: Long, gender: Int): Member {
        if (gender !in GENDER_ALLOWED) {
            throw BadRequestException("INVALID_GENDER")
        }
        val member = requireMember(uid)
        val updated = member.copy(gender = gender)
        memberLogic.update(updated)
        publishProfileChanged(updated, setOf("gender"))
        return updated
    }

    suspend fun updateBirthday(uid: Long, birthday: String?): Member {
        val normalized = birthday?.trim()?.takeIf { it.isNotEmpty() }
        if (normalized != null) {
            if (!BIRTHDAY_REGEX.matches(normalized)) {
                throw BadRequestException("INVALID_BIRTHDAY_FORMAT")
            }
            val year = normalized.substring(0, 4).toInt()
            val currentYear = currentYear()
            if (year < 1900 || year > currentYear) {
                throw BadRequestException("INVALID_BIRTHDAY_FORMAT")
            }
        }
        val member = requireMember(uid)
        val updated = member.copy(birthday = normalized)
        memberLogic.update(updated)
        publishProfileChanged(updated, setOf("birthday"))
        return updated
    }

    private suspend fun requireMember(uid: Long): Member =
        memberLogic.get(uid) ?: throw NotFoundException("MEMBER_NOT_FOUND: $uid")

    private suspend fun publishProfileChanged(after: Member, changedFields: Set<String>) {
        hookBus?.publish(
            MemberProfileChangedHook(
                uid = after.id,
                changedFields = changedFields,
                after = MemberProfileSnapshot(
                    username = after.username,
                    nickname = after.nickname,
                    avatar = after.avatar,
                    mobile = after.mobile,
                    gender = after.gender,
                    bio = after.bio,
                    birthday = after.birthday,
                ),
            ),
        )
    }

    data class UsernameUpdateResult(
        val member: Member,
        val nextChangeAvailableAt: Long,
    )

    companion object {
        /** ^[a-z][a-z0-9_]{2,31}$ */
        private val USERNAME_REGEX = Regex("^[a-z][a-z0-9_]{2,31}$")

        /** spec MODULE_MEMBER_PROFILE_SPEC §4.4 — 保留词最小集，可后续在配置中扩展。 */
        private val USERNAME_RESERVED: Set<String> = setOf(
            "admin", "administrator", "system", "support", "help",
            "root", "owner", "moderator", "mod", "bot",
            "null", "undefined", "none", "test", "user",
            "anonymous", "guest", "official", "staff",
            "privchat", "service",
        )

        private const val USERNAME_RATE_LIMIT_MS = 30L * 24 * 3600 * 1000

        private val BIRTHDAY_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")

        private val GENDER_ALLOWED = setOf(0, 1, 2, 9)

        private fun nowMillis(): Long = Clock.System.now().toEpochMilliseconds()

        private fun currentYear(): Int {
            val now = Clock.System.now()
            // year-month-day 解析靠 ISO string 切片；不引 datetime 重依赖
            val iso = now.toString()           // e.g. 2026-05-04T12:34:56.789Z
            return iso.substring(0, 4).toInt()
        }

        /** 粗判 UNIQUE 约束冲突。SQLite / PostgreSQL / MySQL 错误信息形态不一，靠关键词。 */
        private fun isUniqueViolation(e: Throwable, target: String): Boolean {
            val msg = (e.message ?: "").lowercase()
            // PostgreSQL: "duplicate key value violates unique constraint \"uq_member_users_username\""
            // SQLite:     "unique constraint failed: member_users.username"
            // MySQL:      "duplicate entry '...' for key 'uq_member_users_username'"
            return ("unique" in msg || "duplicate" in msg) && target in msg
        }
    }
}

