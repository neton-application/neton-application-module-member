package model

import kotlinx.serialization.Serializable
import neton.database.annotations.Table
import neton.database.annotations.Id
import neton.database.annotations.SoftDelete
import neton.database.annotations.CreatedAt
import neton.database.annotations.UpdatedAt

@Serializable
@Table("member_addresses")
data class Address(
    @Id
    val id: Long = 0,
    val userId: Long,
    val name: String,
    val mobile: String,
    val areaCode: String? = null,
    val detailAddress: String,
    val defaultStatus: Int = 0,
    @SoftDelete
    val deleted: Int = 0,
    @CreatedAt
    val createdAt: String? = null,
    @UpdatedAt
    val updatedAt: String? = null
)
