package com.nova.runtime.ai.native.indexing

import com.nova.runtime.ai.native.storage.CosineVectorIndex
import com.nova.runtime.models.RuntimeModule
import com.nova.runtime.storage.repository.EmbeddingRepository
import com.nova.runtime.storage.vector.VectorBlobCodec
import com.nova.runtime.utils.logging.NovaLogger
import java.util.UUID

/** Loads persisted embedding vectors from Room into [CosineVectorIndex] at startup. */
class VectorIndexHydrator(
    private val embeddingRepository: EmbeddingRepository,
    private val vectorIndex: CosineVectorIndex,
    private val logger: NovaLogger,
) {
    suspend fun hydrateFromDatabase() {
        var loaded = 0
        var skipped = 0
        var candidates = 0
        var cursor: UUID? = null
        while (true) {
            val entities = embeddingRepository.listPersistedVectorsPage(cursor, PAGE_SIZE)
            if (entities.isEmpty()) break
            candidates += entities.size
            for (entity in entities) {
                val blob = entity.vectorBlob
                val vector = blob?.let { VectorBlobCodec.decode(it, entity.dimension) }
                if (vector == null) {
                    skipped++
                    continue
                }
                vectorIndex.insert(
                    embeddingId = entity.embeddingId,
                    vector = vector,
                    metadata = buildMetadata(entity.objectId, entity.objectType, entity.embeddingKind),
                )
                loaded++
            }
            cursor = entities.last().embeddingId
            if (entities.size < PAGE_SIZE) break
        }

        logger.info(
            module = RuntimeModule.STORAGE.name,
            message = "Vector index hydrated from Room",
            metadata =
                mapOf(
                    "loaded" to loaded.toString(),
                    "skipped" to skipped.toString(),
                    "candidates" to candidates.toString(),
                ),
        )
    }

    companion object {
        const val PAGE_SIZE = 256
    }

    private fun buildMetadata(
        objectId: UUID,
        objectType: String,
        embeddingKind: String?,
    ): Map<String, String> =
        buildMap {
            put(EmbeddingMetadata.OBJECT_ID, objectId.toString())
            put(EmbeddingMetadata.OBJECT_TYPE, objectType)
            if (embeddingKind != null) {
                put(EmbeddingMetadata.EMBEDDING_KIND, embeddingKind)
            }
            putAll(EmbeddingMetadata.passageMetadata(embeddingKind))
        }
}
