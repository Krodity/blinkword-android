package uk.krodity.blinkword.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** Records and exposes day-by-day reading activity. */
class StatsRepository(context: Context) {
    private val readingDayDao = AppDatabase.get(context).readingDayDao()

    fun observeDays(): Flow<List<ReadingDay>> = readingDayDao.observeAll()

    suspend fun record(words: Int, millis: Long, wpm: Int, date: LocalDate = LocalDate.now()) {
        if (words <= 0 && millis <= 0L) return
        readingDayDao.record(date.toString(), words, millis, wpm)
    }
}
