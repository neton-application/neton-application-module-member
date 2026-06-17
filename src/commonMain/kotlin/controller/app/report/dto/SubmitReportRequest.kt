package controller.app.report.dto

import kotlinx.serialization.Serializable

/**
 * 提交举报请求（客户端 → /member/report/submit）。
 *
 * targetType: message / user / channel；targetId 对应实体 id（字符串）。
 * reportedUid: 被举报用户（可选，message/channel 举报时带上便于 admin 聚合）。
 * reason: [enums.ReportReason] 的 code。description / evidenceMessageId 可选。
 */
@Serializable
data class SubmitReportRequest(
    val targetType: String,
    val targetId: String,
    val reason: Int,
    val reportedUid: Long? = null,
    val description: String? = null,
    val evidenceMessageId: Long? = null,
)
