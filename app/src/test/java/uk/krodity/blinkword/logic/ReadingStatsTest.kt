package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.ReadingDay
import java.time.LocalDate

private fun day(date: String, words: Int, millis: Long = 60_000L, maxWpm: Int = 300) =
    ReadingDay(date, words, millis, maxWpm)

private fun doc(id: Long, wordCount: Int, lastReadWordIndex: Int) =
    DocumentSummary(id, "Doc $id", null, "EPUB", null, wordCount, lastReadWordIndex, 0L, 0L)

class ReadingStatsTest {

    private val today = LocalDate.of(2026, 9, 20) // a Sunday

    @Test
    fun `sums all-time totals`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-18", 1000), day("2026-09-20", 1400)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(2400, stats.allTimeWords)
        assertEquals(1400, stats.todayWords)
    }

    @Test
    fun `derives wpm from words over elapsed time`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-20", words = 600, millis = 120_000L)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(300, stats.allTimeWpm)
    }

    @Test
    fun `wpm is zero when no time has been recorded`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-20", words = 600, millis = 0L)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(0, stats.allTimeWpm)
    }

    @Test
    fun `current streak counts back from today`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-18", 10), day("2026-09-19", 10), day("2026-09-20", 10)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(3, stats.currentStreak)
    }

    @Test
    fun `an unread today does not break the streak yet`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-18", 10), day("2026-09-19", 10)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `a gap ends the current streak`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-10", 10), day("2026-09-20", 10)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(1, stats.currentStreak)
    }

    @Test
    fun `best streak finds the longest historical run`() {
        val stats = computeReadingStats(
            days = listOf(
                day("2026-09-01", 10), day("2026-09-02", 10), day("2026-09-03", 10), day("2026-09-04", 10),
                day("2026-09-19", 10), day("2026-09-20", 10),
            ),
            documents = emptyList(),
            today = today,
        )

        assertEquals(4, stats.bestStreak)
        assertEquals(2, stats.currentStreak)
    }

    @Test
    fun `week runs monday to sunday and flags today`() {
        val stats = computeReadingStats(
            days = listOf(day("2026-09-14", 500), day("2026-09-20", 100)),
            documents = emptyList(),
            today = today,
        )

        assertEquals(7, stats.week.size)
        assertEquals(500, stats.week.first().words) // Monday 2026-09-14
        assertEquals(100, stats.week.last().words) // Sunday 2026-09-20
        assertEquals(600, stats.weekWords)
        assertTrue(stats.week.last().isToday)
        assertFalse(stats.week.first().isToday)
    }

    @Test
    fun `books finished counts documents read to the end`() {
        val stats = computeReadingStats(
            days = emptyList(),
            documents = listOf(
                doc(1, wordCount = 100, lastReadWordIndex = 99),
                doc(2, wordCount = 100, lastReadWordIndex = 40),
                doc(3, wordCount = 0, lastReadWordIndex = 0),
            ),
            today = today,
        )

        assertEquals(1, stats.booksFinished)
    }

    @Test
    fun `compact numbers shorten thousands and millions`() {
        assertEquals("164", formatCompactNumber(164))
        assertEquals("2.4K", formatCompactNumber(2431))
        assertEquals("12K", formatCompactNumber(12_000))
        assertEquals("1.2M", formatCompactNumber(1_240_000))
    }

    @Test
    fun `durations read as minutes and hours`() {
        assertEquals("0m", formatDuration(0))
        assertEquals("45m", formatDuration(45 * 60_000L))
        assertEquals("2h 15m", formatDuration(135 * 60_000L))
    }
}
