package com.nova.runtime.app.ui.models

/** A monotonic clock and injectable heap readings keep UI sampling testable. */
class RuntimeTelemetrySampler(
    private val nanoTime: () -> Long = System::nanoTime,
    private val usedHeapBytes: () -> Long = { Runtime.getRuntime().let { it.totalMemory() - it.freeMemory() } },
    private val maxHeapBytes: () -> Long = { Runtime.getRuntime().maxMemory() },
) {
    private val startedAtNanos = nanoTime()

    fun sample(): HeapTelemetrySample {
        val maximum = maxHeapBytes().coerceAtLeast(1L)
        val used = usedHeapBytes().coerceIn(0L, maximum)
        return HeapTelemetrySample(
            memoryUsagePercent = (used.toDouble() / maximum * 100).toInt(),
            sessionUptimeSeconds = ((nanoTime() - startedAtNanos) / 1_000_000_000L).coerceAtLeast(0L),
        )
    }
}

data class HeapTelemetrySample(val memoryUsagePercent: Int, val sessionUptimeSeconds: Long)
