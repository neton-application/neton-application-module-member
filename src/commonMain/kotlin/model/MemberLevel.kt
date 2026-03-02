package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_levels")
data class MemberLevel(
    @Id
    val id: Long = 0,
    val name: String,
    val level: Int,
    val experience: Long = 0,
    val discount: Int = 100,
    val icon: String? = null,
    val status: Int = 0,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
