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
    val status: Int = 0,
    val levelId: Long? = null,
    val experience: Long = 0,
    val point: Int = 0,
    val groupId: Long? = null,
    val registerIp: String? = null,
    val loginIp: String? = null,
    val loginDate: Long? = null,
    @SoftDelete
    val deleted: Int = 0,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
