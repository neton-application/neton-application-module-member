package controller.admin.signin.dto

import kotlinx.serialization.Serializable

@Serializable
data class MemberSignInSummaryVO(
    val totalDay: Int,
    val continuousDay: Int,
    val todaySigned: Boolean
)
