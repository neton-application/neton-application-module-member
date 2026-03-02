package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt

@Serializable
@Table("member_sign_in_records")
data class MemberSignInRecord(
    @Id
    val id: Long = 0,
    val userId: Long,
    val day: Int,
    val point: Int,
    val experience: Int = 0,
    @CreatedAt
    val createdAt: String? = null
)
