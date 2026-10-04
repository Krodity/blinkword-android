package uk.krodity.blinkword.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A fixed-size slice of a [Document]'s text.
 *
 * Room/SQLite caps a single row at roughly 2MB (`CursorWindow`) -- a
 * full-length book stored as one `documents.content` column blows past that
 * and crashes with `SQLiteBlobTooBigException` the moment it's read, not
 * just for oversized epub imports but for any long paste/PDF/markdown too.
 * Splitting the text into bounded chunks keeps every row well under the
 * limit regardless of import source or chapter layout; [DocumentRepository]
 * reassembles them in [chunkIndex] order.
 */
@Entity(
    tableName = "content_chunks",
    foreignKeys = [
        ForeignKey(
            entity = Document::class,
            parentColumns = ["id"],
            childColumns = ["documentId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("documentId")],
)
data class ContentChunk(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val chunkIndex: Int,
    val text: String,
)
