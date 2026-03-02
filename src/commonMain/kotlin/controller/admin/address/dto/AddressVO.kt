package controller.admin.address.dto

import kotlinx.serialization.Serializable

@Serializable
data class AddressVO(
    val id: Long = 0,
    val userId: Long? = null,
    val name: String? = null,
    val mobile: String? = null,
    val areaCode: String? = null,
    val detailAddress: String? = null,
    val defaultStatus: Int? = null,
    val createdAt: String? = null
)
