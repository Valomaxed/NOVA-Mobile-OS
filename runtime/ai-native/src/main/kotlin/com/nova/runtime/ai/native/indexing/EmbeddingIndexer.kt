package com.nova.runtime.ai.native.indexing

import com.nova.runtime.ai.model.EmbeddingGenerator
import com.nova.runtime.ai.model.EmbeddingResult
import com.nova.runtime.ai.model.ImageEmbeddingGenerator
import com.nova.runtime.ai.model.OcrEngine
import com.nova.runtime.ai.model.OcrResult
import com.nova.runtime.ai.native.storage.CosineVectorIndex
import com.nova.runtime.storage.entities.EmbeddingEntity
import com.nova.runtime.storage.repository.EmbeddingRepository
import com.nova.runtime.storage.vector.VectorBlobCodec
import java.util.UUID
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** Coordinates OCR, embedding generation, metadata persistence, and vector indexing. */
class EmbeddingIndexer(
    private val embeddingGenerator: EmbeddingGenerator,
    private val imageEmbeddingGenerator: ImageEmbeddingGenerator,
    private val embeddingRepository: EmbeddingRepository,
    private val vectorIndex: CosineVectorIndex,
    private val ocrEngine: OcrEngine,
) {
    private val documentIndexMutex = Mutex()

    suspend fun hasPassageIndex(embeddingId: UUID): Boolean =
        embeddingRepository.getById(embeddingId)?.embeddingKind == EmbeddingMetadata.KIND_DOCUMENT_V2

    suspend fun markDocumentIndexStale(embeddingId: UUID) {
        embeddingRepository.getById(embeddingId)?.let {
            embeddingRepository.update(it.copy(embeddingKind = "document-summary-stale"))
        }
    }

    /** Prepare every vector before changing the previous searchable document. */
    suspend fun indexDocumentPassages(
        objectId: UUID,
        discoveryText: String,
        body: String,
        commitDocument: suspend (UUID) -> Unit,
    ): Boolean = documentIndexMutex.withLock {
        val windows = try {
            embeddingGenerator.passages(body)
        } catch (error: Exception) {
            if (error is kotlinx.coroutines.CancellationException) throw error
            return@withLock false
        }
        val inputs = listOf(EmbeddingMetadata.KIND_DOCUMENT_V2 to discoveryText) + windows.map {
            "${EmbeddingMetadata.PASSAGE_PREFIX}${it.start}:${it.end}" to it.text
        }
        val prepared = mutableListOf<Pair<EmbeddingEntity, FloatArray>>()
        for ((kind, text) in inputs) {
            currentCoroutineContext().ensureActive()
            val result = embeddingGenerator.embed(text)
            if (result !is EmbeddingResult.Success) return@withLock false
            if (result.vector.isEmpty() || result.vector.size != result.dimension || result.vector.any { !it.isFinite() }) return@withLock false
            prepared += EmbeddingEntity(
                embeddingId = UUID.randomUUID(),
                objectType = "document",
                objectId = objectId,
                modelVersion = result.modelVersion,
                dimension = result.dimension,
                createdAt = System.currentTimeMillis(),
                vectorBlob = VectorBlobCodec.encode(result.vector),
                embeddingKind = kind,
            ) to result.vector
        }
        val previous = embeddingRepository.getByObjectId(objectId).filter { it.objectType == "document" }
        currentCoroutineContext().ensureActive()
        // Once publishing begins, cancellation must not leave Room pointing at removed vectors.
        withContext(NonCancellable) {
            val inserted = mutableListOf<UUID>()
            try {
                for ((entity, _) in prepared) {
                    embeddingRepository.insert(entity)
                    inserted += entity.embeddingId
                }
                commitDocument(prepared.first().first.embeddingId)
            } catch (error: Exception) {
                inserted.forEach { id -> embeddingRepository.delete(id) }
                throw error
            }
            for ((entity, vector) in prepared) {
                vectorIndex.insert(
                    entity.embeddingId, vector,
                    buildMetadata(objectId, "document", entity.embeddingKind),
                )
            }
            for (entity in previous) {
                vectorIndex.delete(entity.embeddingId)
                embeddingRepository.delete(entity.embeddingId)
            }
        }
        true
    }

    suspend fun indexText(
        objectId: UUID,
        objectType: String,
        text: String,
        embeddingKind: String? = null,
        replaceExisting: Boolean = false,
    ): UUID? {
        val embeddingId = UUID.randomUUID()
        return when (val result = embeddingGenerator.embed(text)) {
            is EmbeddingResult.Success -> {
                if (replaceExisting) vectorIndex.deleteByObjectId(objectId)
                persistAndIndex(
                    embeddingId = embeddingId,
                    objectId = objectId,
                    objectType = objectType,
                    vector = result.vector,
                    modelVersion = result.modelVersion,
                    dimension = result.dimension,
                    embeddingKind = embeddingKind,
                )
            }
            is EmbeddingResult.Failure -> null
        }
    }

    suspend fun indexPhoto(
        objectId: UUID,
        imageBytes: ByteArray,
    ): IndexPhotoResult {
        val ocrResult = ocrEngine.recognize(imageBytes)
        val ocrText = when (ocrResult) {
            is OcrResult.Success -> ocrResult.text
            is OcrResult.Failure -> ""
        }

        val ocrEmbeddingId = ocrText
            .takeIf { it.isNotBlank() }
            ?.let { indexText(objectId, OBJECT_TYPE_PHOTO, it, EmbeddingMetadata.KIND_OCR) }

        val imageEmbeddingId = indexImage(objectId, imageBytes)

        return IndexPhotoResult(
            ocrText = ocrText.takeIf { it.isNotBlank() },
            embeddingId = imageEmbeddingId ?: ocrEmbeddingId,
            ocrEmbeddingId = ocrEmbeddingId,
            imageEmbeddingId = imageEmbeddingId,
        )
    }

    private suspend fun indexImage(
        objectId: UUID,
        imageBytes: ByteArray,
    ): UUID? {
        val embeddingId = UUID.randomUUID()
        return when (val result = imageEmbeddingGenerator.embedImage(imageBytes)) {
            is EmbeddingResult.Success -> {
                persistAndIndex(
                    embeddingId = embeddingId,
                    objectId = objectId,
                    objectType = OBJECT_TYPE_PHOTO,
                    vector = result.vector,
                    modelVersion = result.modelVersion,
                    dimension = result.dimension,
                    embeddingKind = EmbeddingMetadata.KIND_IMAGE,
                )
            }
            is EmbeddingResult.Failure -> null
        }
    }

    private suspend fun persistAndIndex(
        embeddingId: UUID,
        objectId: UUID,
        objectType: String,
        vector: FloatArray,
        modelVersion: String,
        dimension: Int,
        embeddingKind: String?,
    ): UUID {
        embeddingRepository.insert(
            EmbeddingEntity(
                embeddingId = embeddingId,
                objectType = objectType,
                objectId = objectId,
                modelVersion = modelVersion,
                dimension = dimension,
                createdAt = System.currentTimeMillis(),
                vectorBlob = VectorBlobCodec.encode(vector),
                embeddingKind = embeddingKind,
            ),
        )
        vectorIndex.insert(
            embeddingId = embeddingId,
            vector = vector,
            metadata = buildMetadata(objectId, objectType, embeddingKind),
        )
        return embeddingId
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

    companion object {
        const val OBJECT_TYPE_PHOTO = "photo"
    }
}

data class IndexPhotoResult(
    val ocrText: String?,
    /** Primary embedding tracked on [com.nova.runtime.storage.entities.PhotoEntity]. */
    val embeddingId: UUID?,
    val ocrEmbeddingId: UUID? = null,
    val imageEmbeddingId: UUID? = null,
)
