package com.nova.runtime.ai.embedding

import org.junit.jupiter.api.Tag
import java.io.File
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.test.Test
import kotlin.test.assertEquals

/** Host-JVM microbenchmark, excluded from normal tests. No performance assertions. */
@Tag("benchmark")
class VectorSearchBenchmarkTest {
    @Test
    fun compareFullSortAndBoundedSelection() {
        val rows = mutableListOf("vectors,dimension,k,baseline_p50_ms,heap_p50_ms,baseline_p95_ms,heap_p95_ms")
        for (size in listOf(1000, 10000, 50000)) {
            val random = Random(2026)
            val index = InMemoryVectorIndex()
            repeat(size) { i -> index.insert(UUID(0L, i.toLong()), FloatArray(384) { random.nextFloat() * 2f - 1f }) }
            val baselineEntries = ConcurrentHashMap<UUID, VectorIndexEntry>()
            index.snapshot().forEach { baselineEntries[it.embeddingId] = it }
            val requests = List(25) { VectorSearchQuery(FloatArray(384) { random.nextFloat() * 2f - 1f }, 10) }
            requests.take(5).forEach { assertEquals(fullSort(baselineEntries.values, it), index.search(it)) }
            repeat(10) {
                fullSort(baselineEntries.values, requests[it])
                index.search(requests[it])
            }
            val baseline = mutableListOf<Long>()
            val optimized = mutableListOf<Long>()
            repeat(25) { i ->
                // Alternate measurement order to reduce warm-up/order bias.
                if (i % 2 == 0) {
                    baseline += measureNanoTime { fullSort(baselineEntries.values, requests[i]) }
                    optimized += measureNanoTime { index.search(requests[i]) }
                } else {
                    optimized += measureNanoTime { index.search(requests[i]) }
                    baseline += measureNanoTime { fullSort(baselineEntries.values, requests[i]) }
                }
            }

            fun percentile(
                values: List<Long>,
                percentile: Double,
            ): Double = values.sorted()[kotlin.math.ceil(values.size * percentile).toInt() - 1] / 1_000_000.0
            rows +=
                listOf(
                    size, 384, 10,
                    percentile(baseline, 0.5), percentile(optimized, 0.5),
                    percentile(baseline, 0.95), percentile(optimized, 0.95),
                ).joinToString(",")
        }
        val report = File(System.getProperty("nova.benchmark.report"))
        report.parentFile.mkdirs()
        report.writeText(rows.joinToString("\n", postfix = "\n"))
        println(report.readText())
    }
}
