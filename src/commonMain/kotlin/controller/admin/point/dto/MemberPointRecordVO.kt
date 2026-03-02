package controller.admin.point.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberPointRecordVO(
    val id: Long = 0,
    val userId: Long? = null,
    val nickname: String? = null,
    val bizType: Int? = null,
    val bizId: String? = null,
    val title: String? = null,
    val point: Int? = null,
    val totalPoint: Int? = null,
    val description: String? = null,
    val createdAt: String? = null
)
