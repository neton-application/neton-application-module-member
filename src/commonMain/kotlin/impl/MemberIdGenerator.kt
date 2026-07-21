package impl

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * 本地 member id 生成器（Snowflake）。builtin 模式下 member_users.id 由本地生成
 * （member_users.id 是显式 bigint，非自增），时间有序、无 DB 竞争、多实例安全。
 *
 * 布局：`(ts-epoch) << 22 | machineId(10) << 12 | seq(12)`。
 */
class MemberIdGenerator(
    private val machineId: Long = 0,
    private val epoch: Long = 1_700_000_000_000, // 2023-11-14
) {
    private val mutex = Mutex()
    private var lastTs = 0L
    private var seq = 0L

    @OptIn(ExperimentalTime::class)
    suspend fun next(): Long = mutex.withLock {
        var ts = Clock.System.now().toEpochMilliseconds()
        if (ts == lastTs) {
            seq = (seq + 1) and 0xFFF
            if (seq == 0L) { // 同毫秒序列耗尽，自旋到下一毫秒
                while (ts <= lastTs) ts = Clock.System.now().toEpochMilliseconds()
            }
        } else {
            seq = 0L
        }
        lastTs = ts
        ((ts - epoch) shl 22) or ((machineId and 0x3FF) shl 12) or seq
    }
}
