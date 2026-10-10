package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

/**
 * 会员档案：一个账号在会员体系里的状态（等级、经验、积分余额、分组）。
 *
 * 与账号一一对应（[userId] = account_users.id），但**不是账号的一部分** —— 账号是很多业务的
 * 地基，要干净而安全；成长数据的读写（签到、刷积分之类的高频、易被滥用的路径）全部落在这张
 * 表上，碰不到登录认证。
 *
 * 档案是**惰性创建**的：从没产生过成长数据的账号没有这一行，读取时按全默认值处理
 * （见 [logic.MemberLogic.profile]）。
 */
@Serializable
@Table("member_profiles")
data class MemberProfile(
    @Id
    val id: Long = 0,
    val userId: Long,
    val levelId: Long? = null,
    val experience: Long = 0,
    /** 积分余额。流水在 member_point_records，余额只能经 [logic.MemberLogic] 的原子更新改动。 */
    val point: Int = 0,
    val groupId: Long? = null,
    @CreatedAt
    val createdAt: Long? = null,
    @UpdatedAt
    val updatedAt: Long? = null,
)
