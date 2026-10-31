package com.nova.runtime.ai.native.indexing

import com.nova.runtime.ai.model.EmbeddingGenerator
import com.nova.runtime.ai.model.EmbeddingResult
import com.nova.runtime.ai.model.HashEmbeddingGenerator
import com.nova.runtime.ai.model.HashImageEmbeddingGenerator
import com.nova.runtime.ai.model.OcrEngine
import com.nova.runtime.ai.model.OcrResult
import com.nova.runtime.ai.native.storage.CosineVectorIndex
import com.nova.runtime.storage.entities.EmbeddingEntity
import com.nova.runtime.storage.repository.EmbeddingRepository
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class DocumentPassageIndexTest {
    private val records = mutableMapOf<UUID, EmbeddingEntity>()
    private val vectors = CosineVectorIndex()
    private var calls = 0
    private var failAt = Int.MAX_VALUE
    private var unavailableTokenizer = false
    private val generator = object : EmbeddingGenerator by HashEmbeddingGenerator(32) {
        override fun passages(text: String): List<com.nova.runtime.ai.tokenizer.TextPassage> {
            check(!unavailableTokenizer) { "Tokenizer assets unavailable" }
            return HashEmbeddingGenerator(32).passages(text)
        }
        override suspend fun embed(text: String): EmbeddingResult {
            calls++
            if (calls == failAt) return EmbeddingResult.Failure("test failure")
            return HashEmbeddingGenerator(32).embed(text)
        }
    }
    private val repository = object : EmbeddingRepository {
        override suspend fun insert(embedding: EmbeddingEntity) { records[embedding.embeddingId] = embedding }
        override suspend fun update(embedding: EmbeddingEntity) { records[embedding.embeddingId] = embedding }
        override suspend fun delete(embeddingId: UUID) { records.remove(embeddingId) }
        override suspend fun getById(embeddingId: UUID) = records[embeddingId]
        override fun observeById(embeddingId: UUID) = emptyFlow<EmbeddingEntity?>()
        override suspend fun listWithPersistedVectors() = records.values.toList()
        override suspend fun listPersistedVectorsPage(afterId: UUID?, limit: Int) = records.values
            .sortedBy { it.embeddingId.toString() }.filter { afterId == null || it.embeddingId.toString() > afterId.toString() }.take(limit)
    }
    private fun indexer() = EmbeddingIndexer(
        generator, HashImageEmbeddingGenerator(32), repository, vectors,
        object : OcrEngine { override suspend fun recognize(imageBytes: ByteArray) = OcrResult.Failure("unused") },
    )

    @Test
    fun longBodyHasPersistedPassageOffsetsAndReplacementRemovesStaleVectors() = runTest {
        val indexer = indexer()
        val objectId = UUID.randomUUID()
        var primary: UUID? = null
        val body = "A long engineering explanation. ".repeat(150) + "QUARTZ-921"
        assertTrue(indexer.indexDocumentPassages(objectId, "Engineering notes", body) { primary = it })
        assertTrue(indexer.hasPassageIndex(requireNotNull(primary)))
        assertTrue(records.size > 2)
        val kinds = records.values.mapNotNull { it.embeddingKind }
        assertTrue(kinds.any { EmbeddingMetadata.passageMetadata(it)[EmbeddingMetadata.PASSAGE_END] == body.length.toString() })
        val previous = records.keys.toSet()
        assertTrue(indexer.indexDocumentPassages(objectId, "Revised notes", "A short revised paragraph.") { primary = it })
        assertTrue(records.keys.none { it in previous })
        assertEquals(2, records.size)
        assertEquals(records.size, vectors.size())
    }

    @Test
    fun unavailableTokenizerLeavesOldIndexSearchable() = runTest {
        val indexer = indexer()
        val objectId = UUID.randomUUID()
        assertTrue(indexer.indexDocumentPassages(objectId, "Old notes", "Existing readable content.") {})
        val previous = records.keys.toSet()
        unavailableTokenizer = true
        assertFalse(indexer.indexDocumentPassages(objectId, "New notes", "Replacement content.") {})
        assertEquals(previous, records.keys.toSet())
    }

    @Test
    fun failedLatePassagePreservesAllPreviouslyPublishedRecords() = runTest {
        val indexer = indexer()
        val objectId = UUID.randomUUID()
        var primary: UUID? = null
        assertTrue(indexer.indexDocumentPassages(objectId, "Old notes", "A valid old paragraph.") { primary = it })
        val previous = records.keys.toSet()
        failAt = calls + 3
        var committed = false
        assertFalse(indexer.indexDocumentPassages(objectId, "New notes", "New long paragraph. ".repeat(200)) { committed = true })
        assertFalse(committed)
        assertEquals(previous, records.keys.toSet())
        assertEquals(previous.size, vectors.size())
        indexer.markDocumentIndexStale(requireNotNull(primary))
        assertFalse(indexer.hasPassageIndex(requireNotNull(primary)))
        assertEquals(previous, records.keys.toSet())
    }
}
