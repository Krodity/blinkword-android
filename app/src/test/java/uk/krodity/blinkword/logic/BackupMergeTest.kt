package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.krodity.blinkword.data.ReadingDay

private fun day(date: String, words: Int, millis: Long = 1000L, wpm: Int = 300) =
    ReadingDay(date, words, millis, wpm)

class BackupMergeTest {

    @Test
    fun `restoring onto an empty device returns the backup as-is`() {
        val incoming = listOf(day("2026-09-19", 500), day("2026-09-20", 800))

        assertEquals(incoming, mergeReadingDays(existing = emptyList(), incoming = incoming))
    }

    @Test
    fun `keeps the higher figure for a day read on both devices`() {
        val merged = mergeReadingDays(
            existing = listOf(day("2026-09-20", words = 900, millis = 5_000L, wpm = 250)),
            incoming = listOf(day("2026-09-20", words = 400, millis = 9_000L, wpm = 600)),
        )

        assertEquals(1, merged.size)
        assertEquals(900, merged.first().wordsRead)
        assertEquals(9_000L, merged.first().millisRead)
        assertEquals(600, merged.first().maxWpm)
    }

    @Test
    fun `importing the same backup twice changes nothing`() {
        val incoming = listOf(day("2026-09-20", 800))
        val once = mergeReadingDays(emptyList(), incoming)

        assertEquals(once, mergeReadingDays(once, incoming))
    }

    @Test
    fun `an older backup never rewinds reading position`() {
        assertEquals(4200, mergeProgress(currentIndex = 4200, backupIndex = 1200))
    }

    @Test
    fun `a newer backup moves reading position forward`() {
        assertEquals(4200, mergeProgress(currentIndex = 0, backupIndex = 4200))
    }
}
