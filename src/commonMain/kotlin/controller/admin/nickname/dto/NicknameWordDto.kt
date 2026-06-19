package controller.admin.nickname.dto

import kotlinx.serialization.Serializable

/**
 * 词条列表项 —— admin list 用。
 */
@Serializable
data class NicknameWordRow(
    val id: Long,
    val word: String,
    /** 0 禁用 / 1 启用 */
    val status: Int,
    val createdAt: Long?,
    val updatedAt: Long?,
)

/**
 * 词条分页查询 —— admin list 用。
 */
@Serializable
data class NicknameWordPageResponse(
    val list: List<NicknameWordRow>,
    val total: Long,
    val page: Int,
    val pageSize: Int,
)

/**
 * 批量导入请求 —— admin 后台 .txt 上传后由 client 拼成 JSON 传入。
 * 每行一词,服务端去重 (`ON CONFLICT (word) DO NOTHING`)。
 */
@Serializable
data class ImportNicknameWordsRequest(
    val words: List<String>,
)

/**
 * 批量导入响应 —— 告诉 admin 这次 import 实际新增了几个 (跳过几个已存在)。
 */
@Serializable
data class ImportNicknameWordsResponse(
    val received: Int,
    val inserted: Int,
    val skipped: Int,
)

/**
 * 单条更新 status (启用 / 禁用) 请求。
 */
@Serializable
data class UpdateNicknameWordStatusRequest(
    /** 0 禁用 / 1 启用 */
    val status: Int,
)
