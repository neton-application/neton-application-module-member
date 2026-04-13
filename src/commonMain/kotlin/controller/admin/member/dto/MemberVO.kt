package controller.admin.member.dto

import kotlinx.serialization.Serializable
import neton.validation.annotations.Max
import neton.validation.annotations.Min
import neton.validation.annotations.NotBlank
import neton.validation.annotations.Pattern
import neton.validation.annotations.Size

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

@Serializable
data class UpdateMemberRequest(
    @property:Min(1)
    val id: Long,

    @property:Pattern("^1[3-9]\\d{9}$")
    val mobile: String? = null,

    @property:NotBlank
    @property:Size(min = 1, max = 64)
    val nickname: String,

    @property:Size(min = 0, max = 255)
    val avatar: String? = null,

    @property:Min(0)
    @property:Max(1)
    val status: Int = 1,

    @property:Min(0)
    val levelId: Long? = null,

    @property:Min(0)
    val groupId: Long? = null
)
