package uk.krodity.blinkword.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * One day's reading activity, accumulated as RSVP playback advances. Daily
 * rows (rather than per-session rows) are all the stats screen needs, and they
 * keep the table bounded to one row per day read.
 */
@Entity(tableName = "reading_days")
data class ReadingDay(
    /** Local-time ISO date, `yyyy-MM-dd`. */
    @PrimaryKey val date: String,
    val wordsRead: Int,
    val millisRead: Long,
    /** Fastest WPM setting used that day, for speed-based achievements. */
    val maxWpm: Int,
)
