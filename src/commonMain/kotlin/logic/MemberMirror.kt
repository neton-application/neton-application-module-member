package logic

import io.github.smyrgeorge.sqlx4k.Statement
import kotlin.time.Clock
import model.Member
import neton.database.adapter.sqlx.SqlxDatabase

/**
 * Caller-provided id 写入 `member_users`，绕过 [SqlxTableAdapter.insert] 的 `id` 剥离。
 *
 * 框架默认 `insert(entity)` 会 `filterKeys { it != "id" }` 让 DB 用 BIGSERIAL 自增；
 * privchat fork 要求 `member_users.id = privchat-server 分配的 uid`（spec UPSTREAM §5、
 * ACCOUNT_IDENTITY §4.1），所以这里走原生 INSERT。`created_at` / `updated_at` 与
 * `mergeAutoFillForInsert` 行为对齐（epoch millis）。
 *
 * 仅 `MemberAuthLogic` 自动注册分支调用，作为 fork-divergent 代码集中在一处。
 */
internal suspend fun insertMemberWithProvidedId(member: Member): Member {
    val now = Clock.System.now().toEpochMilliseconds()
    val sql = """
        INSERT INTO member_users (
            id, mobile, password, username, username_updated_at, nickname, avatar, status,
            level_id, experience, point, group_id,
            register_ip, login_ip, login_date, deleted,
            created_at, updated_at
        ) VALUES (
            :id, :mobile, :password, :username, :usernameUpdatedAt, :nickname, :avatar, :status,
            :levelId, :experience, :point, :groupId,
            :registerIp, :loginIp, :loginDate, :deleted,
            :createdAt, :updatedAt
        )
    """.trimIndent()
    val stmt = Statement.create(sql)
        .bind("id", member.id)
        .bind("mobile", member.mobile)
        .bind("password", member.password)
        .bind("username", member.username)
        .bind("usernameUpdatedAt", member.usernameUpdatedAt)
        .bind("nickname", member.nickname)
        .bind("avatar", member.avatar)
        .bind("status", member.status)
        .bind("levelId", member.levelId)
        .bind("experience", member.experience)
        .bind("point", member.point)
        .bind("groupId", member.groupId)
        .bind("registerIp", member.registerIp)
        .bind("loginIp", member.loginIp)
        .bind("loginDate", member.loginDate)
        .bind("deleted", member.deleted)
        .bind("createdAt", now)
        .bind("updatedAt", now)
    SqlxDatabase.require().execute(stmt).getOrThrow()
    return member.copy(createdAt = now, updatedAt = now)
}
