package com.nova.runtime.ai.embedding

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.PriorityQueue
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.min

/** DPS §7 — brute-force cosine KNN over in-memory float vectors (MVP). */
data class VectorIndexEntry(
    val embeddingId: UUID,
    val vector: FloatArray,
    val metadata: Map<String, String>,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorIndexEntry) return false
        return embeddingId == other.embeddingId &&
            vector.contentEquals(other.vector) &&
            metadata == other.metadata
    }

    override fun hashCode(): Int {
        var result = embeddingId.hashCode()
        result = 31 * result + vector.contentHashCode()
        result = 31 * result + metadata.hashCode()
        return result
    }
}

data class VectorSearchQuery(
    val queryVector: FloatArray,
    val k: Int,
    val metadataFilter: Map<String, String> = emptyMap(),
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is VectorSearchQuery) return false
        return queryVector.contentEquals(other.queryVector) &&
            k == other.k &&
            metadataFilter == other.metadataFilter
    }

    override fun hashCode(): Int {
        var result = queryVector.contentHashCode()
        result = 31 * result + k
        result = 31 * result + metadataFilter.hashCode()
        return result
    }
}

data class VectorSearchHit(
    val embeddingId: UUID,
    val score: Float,
    val metadata: Map<String, String>,
)

data class VectorSearchStats(
    val completedSearches: Long = 0,
    val lastDurationNanos: Long? = null,
    val lastCandidatesScored: Int = 0,
)

class InMemoryVectorIndex {
    private val _searchStats = MutableStateFlow(VectorSearchStats())
    val searchStats = _searchStats.asStateFlow()

    private val entries = ConcurrentHashMap<UUID, VectorIndexEntry>()

    fun insert(
        embeddingId: UUID,
        vector: FloatArray,
        metadata: Map<String, String> = emptyMap(),
    ) {
        entries[embeddingId] =
            VectorIndexEntry(
                embeddingId = embeddingId,
                vector = VectorMath.l2Normalize(vector.copyOf()),
                metadata = metadata,
            )
    }

    fun search(request: VectorSearchQuery): List<VectorSearchHit> {
        val startedAt = System.nanoTime()
        val normalizedQuery = VectorMath.l2Normalize(request.queryVector.copyOf())
        val topK = request.k.coerceAtLeast(1)

        // A worst-first heap retains only k candidates. The encounter ordinal preserves
        // the previous stable sort's order for equal scores, including signed zero/NaN.
        val retained = PriorityQueue<RankedEntry>(compareBy<RankedEntry> { it.score }.thenByDescending { it.ordinal })
        var ordinal = 0L
        for (entry in entries.values) {
            if (!request.metadataFilter.all { (key, value) -> entry.metadata[key] == value }) continue
            val score = VectorMath.cosineSimilarity(normalizedQuery, entry.vector)
            val currentOrdinal = ordinal++
            val worst = retained.peek()
            if (retained.size < topK || score.compareTo(worst.score) > 0) {
                if (retained.size == topK) retained.poll()
                retained.add(RankedEntry(entry, score, currentOrdinal))
            }
        }
        val result =
            retained.sortedWith(compareByDescending<RankedEntry> { it.score }.thenBy { it.ordinal })
                .map { VectorSearchHit(it.entry.embeddingId, it.score, it.entry.metadata) }
        _searchStats.update {
            VectorSearchStats(it.completedSearches + 1, System.nanoTime() - startedAt, ordinal.toInt())
        }
        return result
    }

    private data class RankedEntry(val entry: VectorIndexEntry, val score: Float, val ordinal: Long)

    fun delete(embeddingId: UUID) {
        entries.remove(embeddingId)
    }

    fun deleteByObjectId(objectId: UUID) {
        val objectIdString = objectId.toString()
        entries.entries.removeIf { (_, entry) -> entry.metadata["objectId"] == objectIdString }
    }

    fun size(): Int = entries.size

    fun clear() = entries.clear()

    fun snapshot(limit: Int = Int.MAX_VALUE): List<VectorIndexEntry> = entries.values.take(min(limit, entries.size))
}
