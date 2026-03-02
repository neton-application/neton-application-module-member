package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("member_point_records")
data class MemberPointRecord(
    @Id
    val id: Long = 0,
    val userId: Long,
    val bizType: Int,
    val bizId: String? = null,
    val title: String,
    val point: Int,
    val totalPoint: Int = 0,
    val description: String? = null,
    @CreatedAt
    val createdAt: String? = null
)
