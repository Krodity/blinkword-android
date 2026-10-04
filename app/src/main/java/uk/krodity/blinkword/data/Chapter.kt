package uk.krodity.blinkword.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A chapter boundary within a [Document]'s flattened `content`/token stream.
 * Chapters don't store their own text -- [startWordIndex] is an offset into
 * the document's single continuous RSVP token list, so playback, scrubbing,
 * and progress persistence all keep working unmodified; "jump to chapter"
 * is just `jumpTo(startWordIndex)`.
 */
@Entity(
    tableName = "chapters",
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
data class Chapter(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val documentId: Long,
    val orderIndex: Int,
    val title: String,
    val startWordIndex: Int,
)
