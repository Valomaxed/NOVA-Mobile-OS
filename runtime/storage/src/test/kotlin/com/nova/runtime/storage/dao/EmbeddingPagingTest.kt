package com.nova.runtime.storage.dao

import com.nova.runtime.storage.StorageRobolectricTest
import com.nova.runtime.storage.entities.EmbeddingEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.UUID

class EmbeddingPagingTest : StorageRobolectricTest() {
    @Test
    fun pagesUseDatabaseKeyOrderAndExcludeRowsWithoutVectors() =
        runTest {
            val database = createInMemoryDatabase()
            try {
                val dao = database.embeddingDao()
                val ids = listOf(UUID(Long.MIN_VALUE, 0L), UUID(0L, 3L), UUID(0L, 1L), UUID(-1L, 0L))
                for (id in ids) {
                    dao.insert(EmbeddingEntity(id, "document", UUID.randomUUID(), "test", 1, 1L, byteArrayOf(0, 0, 0, 0)))
                }
                dao.insert(EmbeddingEntity(UUID(0L, 2L), "document", UUID.randomUUID(), "test", 1, 1L))
                val first = dao.listPersistedVectorsPage(null, 2)
                val second = dao.listPersistedVectorsPage(first.last().embeddingId, 2)
                assertEquals(ids.sortedBy { it.toString() }, (first + second).map { it.embeddingId })
                assertEquals(emptyList<EmbeddingEntity>(), dao.listPersistedVectorsPage(second.last().embeddingId, 2))
            } finally {
                database.close()
            }
        }
}
