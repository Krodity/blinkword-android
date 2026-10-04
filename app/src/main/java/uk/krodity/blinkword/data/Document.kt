package uk.krodity.blinkword.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * A document's metadata. The text itself lives in [ContentChunk] rows, not
 * here -- see that class for why.
 */
@Entity(tableName = "documents")
data class Document(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val author: String? = null,
    /** [uk.krodity.blinkword.data.importers.DocumentFormat] name, or "TEXT" for pasted text. */
    val format: String,
    /** Where this came from, when it was downloaded, so a backup can re-fetch it. */
    val sourceUrl: String? = null,
    /** Absolute path to the cover image saved on disk, when the book had one. */
    val coverPath: String? = null,
    val wordCount: Int,
    val lastReadWordIndex: Int = 0,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Library-list projection. */
data class DocumentSummary(
    val id: Long,
    val title: String,
    val author: String?,
    val format: String,
    val coverPath: String?,
    val wordCount: Int,
    val lastReadWordIndex: Int,
    val createdAt: Long,
    val updatedAt: Long,
) {
    val progress: Float
        get() = if (wordCount == 0) 0f else lastReadWordIndex.toFloat() / wordCount
}
