package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

private fun stats(
    allTimeWords: Int = 0,
    maxWpm: Int = 0,
    bestStreak: Int = 0,
    bestDayWords: Int = 0,
    booksFinished: Int = 0,
) = ReadingStats(
    todayWords = 0,
    todayMillis = 0L,
    todayWpm = 0,
    currentStreak = 0,
    bestStreak = bestStreak,
    week = emptyList(),
    weekWords = 0,
    allTimeWords = allTimeWords,
    allTimeMillis = 0L,
    allTimeWpm = 0,
    bestDayWords = bestDayWords,
    maxWpm = maxWpm,
    booksFinished = booksFinished,
)

private fun unlocked(stats: ReadingStats, id: AchievementId) =
    achievements(stats).first { it.id == id }.unlocked

class AchievementsTest {

    @Test
    fun `a fresh install has nothing unlocked`() {
        assertTrue(achievements(stats()).none { it.unlocked })
    }

    @Test
    fun `first words unlock first steps only`() {
        val result = achievements(stats(allTimeWords = 1))
        assertEquals(listOf(AchievementId.FIRST_STEPS), result.filter { it.unlocked }.map { it.id })
    }

    @Test
    fun `speed demon needs 600 wpm`() {
        assertFalse(unlocked(stats(maxWpm = 599), AchievementId.SPEED_DEMON))
        assertTrue(unlocked(stats(maxWpm = 600), AchievementId.SPEED_DEMON))
    }

    @Test
    fun `streak master needs a week`() {
        assertFalse(unlocked(stats(bestStreak = 6), AchievementId.STREAK_MASTER))
        assertTrue(unlocked(stats(bestStreak = 7), AchievementId.STREAK_MASTER))
    }

    @Test
    fun `marathon and centurion track word volume`() {
        assertTrue(unlocked(stats(bestDayWords = 10_000), AchievementId.MARATHON))
        assertTrue(unlocked(stats(allTimeWords = 100_000), AchievementId.CENTURION))
    }

    @Test
    fun `bookworm needs a finished book`() {
        assertTrue(unlocked(stats(booksFinished = 1), AchievementId.BOOKWORM))
    }
}
