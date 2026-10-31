package com.nova.runtime.ai.embedding

import java.util.UUID
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VectorSearchEquivalenceTest {
    @Test
    fun boundedSelection_matchesFullSortAcrossSizesAndFilters() {
        val random = Random(42)
        val index = InMemoryVectorIndex()
        repeat(513) { i ->
            index.insert(
                UUID(0L, i.toLong()),
                FloatArray(32) {
                    random.nextFloat() * 2f - 1f
                },
                mapOf("type" to if (i % 2 == 0) "photo" else "document"),
            )
        }
        val query = FloatArray(32) { random.nextFloat() * 2f - 1f }
        for (k in listOf(-3, 0, 1, 7, 100, 513, Int.MAX_VALUE)) {
            for (filter in listOf(emptyMap(), mapOf("type" to "photo"), mapOf("type" to "absent"))) {
                val request = VectorSearchQuery(query, k, filter)
                assertEquals(fullSort(index, request), index.search(request), "k=$k, filter=$filter")
            }
        }
    }

    @Test
    fun tiesAndZeroVectors_preserveEncounterOrderAndScores() {
        val index = InMemoryVectorIndex()
        repeat(100) { i -> index.insert(UUID(0L, i.toLong()), floatArrayOf(0f, 0f)) }
        for (k in listOf(1, 9, 100, 200)) {
            val query = VectorSearchQuery(floatArrayOf(0f, 0f), k)
            assertEquals(fullSort(index, query), index.search(query))
        }
    }

    @Test
    fun nonFiniteScores_preservePreviousOrdering() {
        val index = InMemoryVectorIndex()
        listOf(
            floatArrayOf(Float.NaN, 0f),
            floatArrayOf(Float.POSITIVE_INFINITY, 0f),
            floatArrayOf(1f, 0f),
            floatArrayOf(-1f, 0f),
        ).forEachIndexed {
                i,
                vector,
            ->
            index.insert(UUID(0L, i.toLong()), vector)
        }
        for (k in listOf(1, 2, 4)) {
            val query = VectorSearchQuery(floatArrayOf(1f, 0f), k)
            assertEquals(fullSort(index, query), index.search(query))
        }
    }

    @Test
    fun mismatchedDimensions_stillFailEvenOutsideTopK() {
        val index = InMemoryVectorIndex()
        index.insert(UUID(0L, 1L), floatArrayOf(1f, 0f))
        index.insert(UUID(0L, 2L), floatArrayOf(0f))
        assertFailsWith<IllegalArgumentException> { index.search(VectorSearchQuery(floatArrayOf(1f, 0f), 1)) }
    }

    @Test
    fun searchStatsTrackActualFilteredCandidatesAndDuration() {
        val index = InMemoryVectorIndex()
        assertNull(index.searchStats.value.lastDurationNanos)
        index.insert(UUID(0L, 1L), floatArrayOf(1f, 0f), mapOf("type" to "photo"))
        index.insert(UUID(0L, 2L), floatArrayOf(0f, 1f), mapOf("type" to "document"))
        index.search(VectorSearchQuery(floatArrayOf(1f, 0f), 1, mapOf("type" to "photo")))
        assertEquals(1L, index.searchStats.value.completedSearches)
        assertEquals(1, index.searchStats.value.lastCandidatesScored)
        assertTrue(index.searchStats.value.lastDurationNanos!! >= 0)
    }

    @Test
    fun emptyIndex_returnsNoResults() {
        assertEquals(emptyList(), InMemoryVectorIndex().search(VectorSearchQuery(floatArrayOf(1f), Int.MAX_VALUE)))
    }
}

/** Reference algorithm retained only in test sources. */
internal fun fullSort(
    index: InMemoryVectorIndex,
    request: VectorSearchQuery,
): List<VectorSearchHit> {
    return fullSort(index.snapshot(), request)
}

internal fun fullSort(
    entries: Collection<VectorIndexEntry>,
    request: VectorSearchQuery,
): List<VectorSearchHit> {
    val query = VectorMath.l2Normalize(request.queryVector.copyOf())
    return entries.asSequence()
        .filter { entry -> request.metadataFilter.all { (key, value) -> entry.metadata[key] == value } }
        .map { VectorSearchHit(it.embeddingId, VectorMath.cosineSimilarity(query, it.vector), it.metadata) }
        .sortedByDescending { it.score }.take(request.k.coerceAtLeast(1)).toList()
}
