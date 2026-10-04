package uk.krodity.blinkword.logic

import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.ReadingDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** One bar of the weekly chart. */
data class DayBar(val label: String, val words: Int, val isToday: Boolean)

data class ReadingStats(
    val todayWords: Int,
    val todayMillis: Long,
    val todayWpm: Int,
    val currentStreak: Int,
    val bestStreak: Int,
    val week: List<DayBar>,
    val weekWords: Int,
    val allTimeWords: Int,
    val allTimeMillis: Long,
    val allTimeWpm: Int,
    val bestDayWords: Int,
    val maxWpm: Int,
    val booksFinished: Int,
)

/**
 * Rolls day rows plus the library into everything the stats screen shows.
 * Pure so the streak/aggregate rules can be tested without a database.
 */
fun computeReadingStats(
    days: List<ReadingDay>,
    documents: List<DocumentSummary>,
    today: LocalDate = LocalDate.now(),
): ReadingStats {
    val byDate = days.associateBy { it.date }
    val todayRow = byDate[today.toString()]

    val weekStart = today.minusDays(((today.dayOfWeek.value + 6) % 7).toLong())
    val week = (0..6).map { offset ->
        val date = weekStart.plusDays(offset.toLong())
        DayBar(
            label = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
            words = byDate[date.toString()]?.wordsRead ?: 0,
            isToday = date == today,
        )
    }

    val allTimeWords = days.sumOf { it.wordsRead }
    val allTimeMillis = days.sumOf { it.millisRead }

    return ReadingStats(
        todayWords = todayRow?.wordsRead ?: 0,
        todayMillis = todayRow?.millisRead ?: 0L,
        todayWpm = wpmFrom(todayRow?.wordsRead ?: 0, todayRow?.millisRead ?: 0L),
        currentStreak = currentStreak(byDate.keys, today),
        bestStreak = bestStreak(days),
        week = week,
        weekWords = week.sumOf { it.words },
        allTimeWords = allTimeWords,
        allTimeMillis = allTimeMillis,
        allTimeWpm = wpmFrom(allTimeWords, allTimeMillis),
        bestDayWords = days.maxOfOrNull { it.wordsRead } ?: 0,
        maxWpm = days.maxOfOrNull { it.maxWpm } ?: 0,
        booksFinished = documents.count { it.wordCount > 0 && it.lastReadWordIndex >= it.wordCount - 1 },
    )
}

private fun wpmFrom(words: Int, millis: Long): Int =
    if (millis <= 0L) 0 else (words * 60_000.0 / millis).roundToInt()

/**
 * Consecutive days read, ending today. A day with no reading yet doesn't break
 * the streak until it's over, so the count starts at yesterday when today is
 * still empty.
 */
private fun currentStreak(datesRead: Set<String>, today: LocalDate): Int {
    var cursor = if (today.toString() in datesRead) today else today.minusDays(1)
    var streak = 0
    while (cursor.toString() in datesRead) {
        streak++
        cursor = cursor.minusDays(1)
    }
    return streak
}

/** The longest run of consecutive days ever read. */
private fun bestStreak(days: List<ReadingDay>): Int {
    val dates = days.map { LocalDate.parse(it.date) }.sorted()
    var best = 0
    var run = 0
    var previous: LocalDate? = null
    for (date in dates) {
        run = if (previous != null && previous.plusDays(1) == date) run + 1 else 1
        best = maxOf(best, run)
        previous = date
    }
    return best
}

/** 164 -> "164", 2431 -> "2.4K", 1_240_000 -> "1.2M". */
fun formatCompactNumber(value: Int): String = when {
    value < 1_000 -> value.toString()
    value < 1_000_000 -> trimZero(value / 1_000.0) + "K"
    else -> trimZero(value / 1_000_000.0) + "M"
}

private fun trimZero(value: Double): String {
    val rounded = (value * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

/** 0 -> "0m", 45 min -> "45m", 135 min -> "2h 15m". */
fun formatDuration(millis: Long): String {
    val totalMinutes = millis / 60_000
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
