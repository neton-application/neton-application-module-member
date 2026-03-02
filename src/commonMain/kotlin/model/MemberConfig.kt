package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_configs")
data class MemberConfig(
    @Id
    val id: Long = 0,
    val configKey: String,
    val value: String,
    @UpdatedAt
    val updatedAt: String? = null
)
