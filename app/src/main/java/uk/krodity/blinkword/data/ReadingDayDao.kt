package uk.krodity.blinkword.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadingDayDao {
    @Query("SELECT * FROM reading_days ORDER BY date")
    fun observeAll(): Flow<List<ReadingDay>>

    @Query("SELECT * FROM reading_days WHERE date = :date")
    suspend fun getDay(date: String): ReadingDay?

    @Query("SELECT * FROM reading_days ORDER BY date")
    suspend fun getAll(): List<ReadingDay>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(day: ReadingDay)

    /** Adds to the day's running totals, creating the row on first read of the day. */
    @Transaction
    suspend fun record(date: String, words: Int, millis: Long, wpm: Int) {
        val existing = getDay(date)
        upsert(
            ReadingDay(
                date = date,
                wordsRead = (existing?.wordsRead ?: 0) + words,
                millisRead = (existing?.millisRead ?: 0) + millis,
                maxWpm = maxOf(existing?.maxWpm ?: 0, wpm),
            )
        )
    }
}
