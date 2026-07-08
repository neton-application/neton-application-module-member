package logic

import model.MemberInviteCode
import model.MemberInviteRecord
import model.Member
import table.MemberInviteCodeTable
import table.MemberInviteRecordTable
import table.MemberTable
import neton.database.api.DbContext
import neton.database.dsl.*
import neton.logging.Logger
import neton.core.http.BadRequestException
import kotlin.time.Clock
import kotlin.random.Random

/**
 * 邀请码(MEMBER_INVITE_CODE v1.0)。
 *
 * 注意:不用 @Logic —— 需要注入可选的 [port.MemberInvitePort](KSP Logic 装配只支持
 * log/db 固定依赖),在 [init.MemberRuntimeBootstrap] 手动装配(MemberSignInLogic 同款)。
 *
 * 关键不变量:
 * - 一个用户只绑定一次(invitee_user_id UNIQUE 幂等锚点)。
 * - used_count 递增与 record 写入同一事务(评审 C:码被消耗则用户必已绑上)。
 * - 自动加好友弱一致:绑定成功、加好友失败 → FAILED 可补偿,不回滚(评审拍板 §2-6)。
 */
class MemberInviteLogic(
    private val log: Logger,
    private val db: DbContext,
    private val invitePort: port.MemberInvitePort? = null,
) {

    // ─────────── 生成与校验 ───────────

    /** 生成 8 位邀请码(排除 0/O/1/I);唯一索引兜底,冲突重试。 */
    private fun randomCode(): String =
        (1..8).map { CODE_ALPHABET[Random.nextInt(CODE_ALPHABET.length)] }.joinToString("")

    /**
     * 校验邀请码可用性;返回码实体。[bindingUserId] 补填场景传入(校验不能绑自己的码)。
     * 任一不满足抛 BadRequestException(统一文案,不泄露具体原因给枚举攻击)。
     */
    suspend fun validate(rawCode: String, bindingUserId: Long? = null): MemberInviteCode {
        val code = rawCode.trim()
        if (code.isEmpty()) throw BadRequestException("INVITE_CODE_INVALID")
        val entity = MemberInviteCodeTable.oneWhere { MemberInviteCode::code eq code }
            ?: throw BadRequestException("INVITE_CODE_INVALID")
        val now = Clock.System.now().toEpochMilliseconds()
        if (entity.status != 1) throw BadRequestException("INVITE_CODE_INVALID")
        if (entity.expiresAt != null && entity.expiresAt <= now) throw BadRequestException("INVITE_CODE_INVALID")
        if (entity.maxUses > 0 && entity.usedCount >= entity.maxUses) throw BadRequestException("INVITE_CODE_INVALID")
        val owner = entity.ownerUserId
        if (owner != null) {
            if (bindingUserId != null && owner == bindingUserId) throw BadRequestException("INVITE_CODE_SELF")
            val inviter = MemberTable.get(owner)
            if (inviter == null || inviter.status == 0) throw BadRequestException("INVITE_CODE_INVALID")
        }
        return entity
    }

    // ─────────── 绑定(注册时 / 注册后补填共用) ───────────

    /**
     * 事务内绑定:原子计数 + 写 record。**必须在 db.transaction 内调用**
     * (注册路径与用户插入同事务;补填路径自带事务)。返回 record(含自增 id)。
     */
    suspend fun applyInTx(
        codeEntity: MemberInviteCode,
        inviteeUserId: Long,
        registerMode: String,
        registerIdentifierMasked: String?,
        bindScene: Int,
    ): MemberInviteRecord {
        // 行级原子计数,防并发超发(评审 C:与 record 同事务,record 失败整体回滚)
        val updated = db.execute(
            "UPDATE member_invite_codes SET used_count = used_count + 1, updated_at = :now " +
                "WHERE id = :id AND (max_uses = 0 OR used_count < max_uses)",
            mapOf("id" to codeEntity.id, "now" to Clock.System.now().toEpochMilliseconds()),
        )
        if (updated == 0L) throw BadRequestException("INVITE_CODE_INVALID")
        val record = MemberInviteRecordTable.insert(
            MemberInviteRecord(
                codeId = codeEntity.id,
                code = codeEntity.code,
                inviterUserId = codeEntity.ownerUserId,
                inviteeUserId = inviteeUserId,
                registerMode = registerMode,
                registerIdentifierMasked = registerIdentifierMasked,
                bindScene = bindScene,
                autoFriendStatus = 0,
                boundAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
        log.info(
            "member.invite.bound",
            mapOf("recordId" to record.id, "code" to codeEntity.code, "invitee" to inviteeUserId, "scene" to bindScene),
        )
        return record
    }

    /**
     * 事务提交后的自动加好友(弱一致):owner 为空 → SKIPPED;
     * port 未装配 → SKIPPED(builtin 模式);失败 → FAILED + error,可补偿。
     */
    suspend fun dispatchAutoFriend(recordId: Long) {
        val record = MemberInviteRecordTable.get(recordId) ?: return
        val inviter = record.inviterUserId
        if (inviter == null || invitePort == null) return // SKIPPED(默认 0)
        try {
            invitePort.autoFriend(inviter, record.inviteeUserId, record.code)
            MemberInviteRecordTable.update(record.copy(autoFriendStatus = 1, autoFriendError = null))
            log.info("member.invite.auto_friend.success", mapOf("recordId" to recordId))
        } catch (e: Exception) {
            MemberInviteRecordTable.update(
                record.copy(autoFriendStatus = 2, autoFriendError = e.message?.take(255)),
            )
            log.warn("member.invite.auto_friend.failed", mapOf("recordId" to recordId, "error" to (e.message ?: "")))
        }
    }

    /** 注册路径绑定:计数+record 同一事务(评审 C)。bindScene=REGISTER。 */
    suspend fun applyInviteForNewUser(
        codeEntity: MemberInviteCode,
        inviteeUserId: Long,
        registerMode: String,
        identifierMasked: String?,
    ): MemberInviteRecord = db.transaction {
        applyInTx(codeEntity, inviteeUserId, registerMode, identifierMasked, bindScene = 1)
    }

    // ─────────── 补填(【我】页) ───────────

    suspend fun myBinding(userId: Long): MemberInviteRecord? =
        MemberInviteRecordTable.oneWhere { MemberInviteRecord::inviteeUserId eq userId }

    /** 注册后补填。已绑定 → 拒绝;成功返回 record。 */
    suspend fun bind(userId: Long, rawCode: String, registerMode: String, identifierMasked: String?): MemberInviteRecord {
        if (myBinding(userId) != null) throw BadRequestException("INVITE_ALREADY_BOUND")
        val entity = validate(rawCode, bindingUserId = userId)
        val record = db.transaction {
            applyInTx(entity, userId, registerMode, identifierMasked, bindScene = 2)
        }
        dispatchAutoFriend(record.id)
        return MemberInviteRecordTable.get(record.id) ?: record
    }

    // ─────────── Admin ───────────

    suspend fun adminCreate(input: MemberInviteCode): MemberInviteCode {
        val explicit = input.code.trim()
        if (explicit.isNotEmpty() && !CODE_REGEX.matches(explicit)) {
            throw BadRequestException("邀请码为 1-32 位字符(不含空白)")
        }
        val now = Clock.System.now().toEpochMilliseconds()
        var attempt = 0
        while (true) {
            val code = explicit.ifEmpty { randomCode() }
            val exists = MemberInviteCodeTable.oneWhere { MemberInviteCode::code eq code }
            if (exists == null) {
                return MemberInviteCodeTable.insert(input.copy(code = code, usedCount = 0, createdAt = now, updatedAt = now))
            }
            if (explicit.isNotEmpty()) throw BadRequestException("邀请码已存在")
            if (++attempt > 5) throw BadRequestException("邀请码生成冲突,请重试")
        }
    }

    suspend fun adminUpdate(input: MemberInviteCode) {
        val existing = MemberInviteCodeTable.get(input.id)
            ?: throw BadRequestException("邀请码不存在")
        // code/owner/usedCount 不可改(改码等于换码;owner 改绑争议大,v1 禁止)
        MemberInviteCodeTable.update(
            existing.copy(
                maxUses = input.maxUses,
                status = input.status,
                expiresAt = input.expiresAt,
                remark = input.remark,
                updatedAt = Clock.System.now().toEpochMilliseconds(),
            ),
        )
    }

    suspend fun adminDelete(id: Long) {
        MemberInviteCodeTable.destroy(id)
    }

    suspend fun listAll(): List<MemberInviteCode> =
        MemberInviteCodeTable.query { orderBy(MemberInviteCode::id.desc()) }.list()

    suspend fun recordsOfCode(codeId: Long): List<MemberInviteRecord> =
        MemberInviteCodeTable.get(codeId)?.let { c ->
            MemberInviteRecordTable.query {
                where { MemberInviteRecord::codeId eq c.id }
                orderBy(MemberInviteRecord::id.desc())
            }.list()
        } ?: emptyList()

    suspend fun recordOfInvitee(inviteeUserId: Long): MemberInviteRecord? = myBinding(inviteeUserId)

    /** 后台补偿:对 FAILED 记录重试自动加好友。 */
    suspend fun retryAutoFriend(recordId: Long) {
        val record = MemberInviteRecordTable.get(recordId)
            ?: throw BadRequestException("邀请记录不存在")
        if (record.autoFriendStatus == 1) return
        dispatchAutoFriend(recordId)
        val after = MemberInviteRecordTable.get(recordId)
        if (after?.autoFriendStatus != 1) {
            throw BadRequestException("补偿失败:${after?.autoFriendError ?: "未知原因"}")
        }
    }

    companion object {
        private const val CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        // 自定义码放宽为任意 1-32 位非空白字符(用户拍板:纯数字如 "1" 也合法);
        // 随机生成仍用 8 位防混淆字母表。精确匹配、区分大小写。
        private val CODE_REGEX = Regex("^\\S{1,32}$")
    }
}
