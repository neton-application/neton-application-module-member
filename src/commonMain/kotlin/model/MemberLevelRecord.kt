package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("member_level_records")
data class MemberLevelRecord(
    @Id
    val id: Long = 0,
    val userId: Long,
    val levelId: Long,
    val level: Int,
    val reason: String? = null,
    val description: String? = null,
    @CreatedAt
    val createdAt: String? = null
)
