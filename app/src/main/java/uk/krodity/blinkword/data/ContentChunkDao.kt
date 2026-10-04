package uk.krodity.blinkword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ContentChunkDao {
    @Query("SELECT * FROM content_chunks WHERE documentId = :documentId ORDER BY chunkIndex")
    suspend fun getForDocument(documentId: Long): List<ContentChunk>

    @Insert
    suspend fun insertAll(chunks: List<ContentChunk>)
}
