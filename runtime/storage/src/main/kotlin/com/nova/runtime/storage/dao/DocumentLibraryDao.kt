package com.nova.runtime.storage.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/** Lightweight library rows: extracted bodies and summaries are loaded only on demand. */
data class DocumentLibraryEntry(
    val id: UUID,
    val name: String,
    val size: Long,
    val contentExtractStatus: String,
    val hasVector: Boolean,
)

@Dao
interface DocumentLibraryDao {
    /** Upgrade older summary-only records in bounded background batches. */
    @Query(
        """
        SELECT documents.* FROM documents
        INNER JOIN embeddings ON documents.embeddingId = embeddings.embeddingId
        WHERE (embeddings.embeddingKind IS NULL OR embeddings.embeddingKind != 'document-summary-v2')
          AND length(trim(coalesce(documents.contentText, ''))) > 0
        ORDER BY documents.modifiedAt DESC LIMIT :limit
        """,
    )
    suspend fun listNeedingPassages(limit: Int): List<com.nova.runtime.storage.entities.DocumentEntity>

    @Query(
        """
        SELECT documents.id, documents.name, documents.size, documents.contentExtractStatus,
               EXISTS(SELECT 1 FROM embeddings
                      WHERE embeddings.embeddingId = documents.embeddingId
                        AND embeddings.vectorBlob IS NOT NULL) AS hasVector
        FROM documents
        ORDER BY documents.modifiedAt DESC, documents.id ASC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<DocumentLibraryEntry>>

    @Query(
        """
        SELECT documents.id, documents.name, documents.size, documents.contentExtractStatus,
               EXISTS(SELECT 1 FROM embeddings
                      WHERE embeddings.embeddingId = documents.embeddingId
                        AND embeddings.vectorBlob IS NOT NULL) AS hasVector
        FROM documents
        WHERE EXISTS(SELECT 1 FROM preferences WHERE preferences.`key` = 'daily_document_' || documents.id)
        ORDER BY documents.modifiedAt DESC, documents.id ASC
        LIMIT :limit
        """,
    )
    fun observeDaily(limit: Int): Flow<List<DocumentLibraryEntry>>
}
