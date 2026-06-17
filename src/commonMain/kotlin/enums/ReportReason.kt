package enums

/**
 * 举报理由枚举（App Store UGC 最小治理）。code 与客户端约定。
 *
 * 最小集合够审核要求：垃圾广告 / 骚扰辱骂 / 色情低俗 / 欺诈违法 / 其他。
 */
enum class ReportReason(val code: Int, val description: String) {
    SPAM(1, "垃圾广告"),
    HARASSMENT(2, "骚扰辱骂"),
    PORN(3, "色情低俗"),
    FRAUD(4, "欺诈违法"),
    OTHER(9, "其他");

    companion object {
        fun isValid(code: Int): Boolean = entries.any { it.code == code }
    }
}

/**
 * 举报目标类型。target_id 对应实体 id（字符串，兼容 message/user/channel）。
 */
object ReportTargetType {
    const val MESSAGE = "message"
    const val USER = "user"
    const val CHANNEL = "channel"

    val ALL = setOf(MESSAGE, USER, CHANNEL)
    fun isValid(type: String): Boolean = type in ALL
}
