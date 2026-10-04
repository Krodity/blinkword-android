package uk.krodity.blinkword.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "collections")
data class Collection(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAt: Long,
)

/** Many-to-many join: a document can belong to more than one collection (or none). */
@Entity(
    tableName = "document_collections",
    primaryKeys = ["documentId", "collectionId"],
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = Collection::class,
            parentColumns = ["id"],
            childColumns = ["collectionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("collectionId")],
)
data class DocumentCollectionCrossRef(
    val documentId: Long,
    val collectionId: Long,
)
