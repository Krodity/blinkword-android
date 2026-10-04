package uk.krodity.blinkword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DocumentDao {

    @Query(
        "SELECT id, title, author, format, coverPath, wordCount, lastReadWordIndex, createdAt, updatedAt " +
            "FROM documents ORDER BY updatedAt DESC"
    )
    fun observeSummaries(): Flow<List<DocumentSummary>>

    @Query("UPDATE documents SET coverPath = :path WHERE id = :id")
    suspend fun updateCoverPath(id: Long, path: String)

    @Query("SELECT * FROM documents WHERE id = :id")
    suspend fun getById(id: Long): Document?

    @Query("SELECT * FROM documents ORDER BY createdAt")
    suspend fun getAll(): List<Document>

    @Query("SELECT * FROM documents WHERE title = :title LIMIT 1")
    suspend fun findByTitle(title: String): Document?

    @Insert
    suspend fun insert(document: Document): Long

    @Query("UPDATE documents SET lastReadWordIndex = :index, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateProgress(id: Long, index: Int, updatedAt: Long)

    @Query("DELETE FROM documents WHERE id = :id")
    suspend fun delete(id: Long)
}
