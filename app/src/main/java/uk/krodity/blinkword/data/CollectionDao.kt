package uk.krodity.blinkword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<Collection>>

    @Query("SELECT * FROM collections ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<Collection>

    @Query("SELECT * FROM document_collections")
    suspend fun getAllCrossRefs(): List<DocumentCollectionCrossRef>

    @Insert
    suspend fun insert(collection: Collection): Long

    @Query("DELETE FROM collections WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM document_collections")
    fun observeCrossRefs(): Flow<List<DocumentCollectionCrossRef>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun assign(crossRef: DocumentCollectionCrossRef)

    @Query("DELETE FROM document_collections WHERE documentId = :documentId AND collectionId = :collectionId")
    suspend fun unassign(documentId: Long, collectionId: Long)
}
