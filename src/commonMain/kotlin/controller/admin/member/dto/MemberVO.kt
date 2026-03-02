package controller.admin.member.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberVO(
    val id: Long = 0,
    val mobile: String? = null,
    val nickname: String? = null,
    val avatar: String? = null,
    val status: Int? = null,
    val levelId: Long? = null,
    val levelName: String? = null,
    val experience: Long? = null,
    val point: Int? = null,
    val groupId: Long? = null,
    val groupName: String? = null,
    val registerIp: String? = null,
    val loginIp: String? = null,
    val loginDate: Long? = null,
    val createdAt: String? = null
)
