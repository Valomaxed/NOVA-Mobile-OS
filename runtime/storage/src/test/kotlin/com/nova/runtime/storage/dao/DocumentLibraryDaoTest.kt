package com.nova.runtime.storage.dao

import com.nova.runtime.storage.StorageRobolectricTest
import com.nova.runtime.storage.entities.DocumentEntity
import com.nova.runtime.storage.entities.EmbeddingEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DocumentLibraryDaoTest : StorageRobolectricTest() {
    @Test
    fun legacyContentIsQueuedUntilPassageIndexIsPublished() = runTest {
        val database = createInMemoryDatabase()
        try {
            val row = document(1, 2).copy(contentText = "Readable full document content.")
            val embedding = EmbeddingEntity(UUID.randomUUID(), "document", row.id, "test", 1, 1L, byteArrayOf(0, 0, 0, 0), "summary")
            database.embeddingDao().insert(embedding)
            database.documentDao().insert(row.copy(embeddingId = embedding.embeddingId))
            assertEquals(listOf(row.id), database.documentLibraryDao().listNeedingPassages(10).map { it.id })
            database.embeddingDao().update(embedding.copy(embeddingKind = "document-summary-stale"))
            assertEquals(listOf(row.id), database.documentLibraryDao().listNeedingPassages(10).map { it.id })
            database.embeddingDao().update(embedding.copy(embeddingKind = "document-summary-v2"))
            assertTrue(database.documentLibraryDao().listNeedingPassages(10).isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun recentRowsAreBoundedAndTiesHaveStableOrder() = runTest {
        val database = createInMemoryDatabase()
        try {
            val older = document(3, 1)
            val first = document(1, 2).copy(contentText = "large body ".repeat(100_000))
            val second = document(2, 2)
            listOf(older, second, first).forEach { database.documentDao().insert(it) }
            assertEquals(listOf(first.id, second.id), database.documentLibraryDao().observeRecent(2).first().map { it.id })
            assertEquals(3, database.documentLibraryDao().observeRecent(100).first().size)
            assertEquals(first.contentText, database.documentDao().getById(first.id)?.contentText)
            assertTrue(database.documentLibraryDao().observeRecent(0).first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun searchableRequiresAPersistedVectorNotJustAnEmbeddingReference() = runTest {
        val database = createInMemoryDatabase()
        try {
            val row = document(1, 2)
            val embeddingId = UUID.randomUUID()
            val embedding = EmbeddingEntity(embeddingId, "document", row.id, "test", 1, 1L, null)
            database.embeddingDao().insert(embedding)
            database.documentDao().insert(row.copy(embeddingId = embeddingId))
            assertFalse(database.documentLibraryDao().observeRecent(100).first().single().hasVector)
            database.embeddingDao().update(embedding.copy(vectorBlob = byteArrayOf(0, 0, 0, 0)))
            assertTrue(database.documentLibraryDao().observeRecent(100).first().single().hasVector)
            database.documentDao().delete(row)
            assertTrue(database.documentLibraryDao().observeRecent(100).first().isEmpty())
        } finally {
            database.close()
        }
    }

    @Test
    fun dailyEssentialsOnlyIncludeSavedExistingDocumentsAndUnsaveKeepsDocument() = runTest {
        val database = createInMemoryDatabase()
        try {
            val first = document(41, 2)
            val other = document(42, 3)
            database.documentDao().insert(first)
            database.documentDao().insert(other)
            assertTrue(database.documentLibraryDao().observeDaily(100).first().isEmpty())
            val saved = com.nova.runtime.storage.entities.PreferenceEntity("daily_document_${first.id}", "true", 1.0f, 1L)
            database.preferenceDao().insert(saved)
            assertEquals(listOf(first.id), database.documentLibraryDao().observeDaily(100).first().map { it.id })
            database.preferenceDao().delete(saved)
            assertTrue(database.documentLibraryDao().observeDaily(100).first().isEmpty())
            assertEquals(first, database.documentDao().getById(first.id))
            database.preferenceDao().insert(saved)
            database.documentDao().delete(first)
            assertTrue(database.documentLibraryDao().observeDaily(100).first().isEmpty())
        } finally { database.close() }
    }

    private fun document(number: Long, modified: Long) = DocumentEntity(
        id = UUID(0, number),
        path = "/documents/$number.txt",
        name = "$number.txt",
        extension = "txt",
        mimeType = "text/plain",
        size = 128,
        checksum = "",
        createdAt = 1,
        modifiedAt = modified,
        indexedAt = null,
        projectId = null,
        embeddingId = null,
        importance = 1,
    )
}
