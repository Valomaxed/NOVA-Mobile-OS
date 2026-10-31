package com.nova.runtime.app.ui.models

data class LiveTelemetryState(
    val memoryUsagePercent: Int = 0,
    val npuTeraOpsCurrent: Float? = null,
    val npuTeraOpsPeak: Float? = null,
    val npuTempCelsius: Int? = null,
    val indexedVectorCount: Int = 0,
    val totalDocumentsIndexed: Int = 0,
    val ragLatencyMs: Double? = null,
    val ragThroughputDocsPerSec: Int? = null,
    val nodeLatencyMs: Float? = null,
    val sessionUptimeSeconds: Long = 0L,
    val totalTasksExecuted: Int = 0,
    val lastCommandLatencyMs: Long? = null,
    val activeQueueProgress: Float? = null,
) {
    val formattedUptime: String
        get() {
            val hours = sessionUptimeSeconds / 3600
            val minutes = (sessionUptimeSeconds % 3600) / 60
            val seconds = sessionUptimeSeconds % 60
            return String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
}
