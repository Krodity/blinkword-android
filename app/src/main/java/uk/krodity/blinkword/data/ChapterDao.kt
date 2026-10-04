package uk.krodity.blinkword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface ChapterDao {
    @Query("SELECT * FROM chapters WHERE documentId = :documentId ORDER BY orderIndex")
    suspend fun getForDocument(documentId: Long): List<Chapter>

    @Insert
    suspend fun insertAll(chapters: List<Chapter>)
}
