package com.nova.runtime.app.ui.models

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RuntimeTelemetrySamplerTest {
    @Test
    fun samplesHeapAndMonotonicUptimeWithoutArtificialClamp() {
        var clock = 0L
        var usedHeap = 0L
        val sampler = RuntimeTelemetrySampler({ clock }, { usedHeap }, { 100L })
        assertEquals(0, sampler.sample().memoryUsagePercent)
        usedHeap = 100L
        clock = 7_000_000_000L
        assertEquals(100, sampler.sample().memoryUsagePercent)
        assertEquals(7L, sampler.sample().sessionUptimeSeconds)
    }

    @Test
    fun unavailableHardwareAndLatencyHaveNoInventedDefaults() {
        val state = LiveTelemetryState()
        assertNull(state.npuTeraOpsCurrent)
        assertNull(state.npuTempCelsius)
        assertNull(state.ragLatencyMs)
        assertNull(state.lastCommandLatencyMs)
        assertEquals(0, state.totalTasksExecuted)
    }
}
