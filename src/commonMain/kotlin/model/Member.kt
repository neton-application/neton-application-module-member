package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.SoftDelete
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_users")
data class Member(
    @Id
    val id: Long = 0,
    val mobile: String? = null,
    val password: String? = null,
    val nickname: String,
    val avatar: String? = null,
    val status: Int = 1,
    val levelId: Long? = null,
    val experience: Long = 0,
    val point: Int = 0,
    val groupId: Long? = null,
    val registerIp: String? = null,
    val loginIp: String? = null,
    val loginDate: Long? = null,
    /** 平台账号名；spec MODULE_MEMBER_PROFILE_SPEC §2。application 是守门人，DB UNIQUE 兜底。 */
    val username: String? = null,
    /** 上次改 username 的 millis 时间戳；30 天限改判断依据。 */
    val usernameUpdatedAt: Long? = null,
    /** 0=unknown / 1=male / 2=female / 9=other */
    val gender: Int = 0,
    /** 个性签名，0–200 字符；null = 清空。 */
    val bio: String? = null,
    /** ISO `YYYY-MM-DD` 字符串；跨端避免 DATE 时区歧义。 */
    val birthday: String? = null,
    @SoftDelete
    val deleted: Int = 0,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
