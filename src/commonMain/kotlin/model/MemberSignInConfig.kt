package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_sign_in_configs")
data class MemberSignInConfig(
    @Id
    val id: Long = 0,
    val day: Int,
    val point: Int,
    val experience: Int = 0,
    val status: Int = 0,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
