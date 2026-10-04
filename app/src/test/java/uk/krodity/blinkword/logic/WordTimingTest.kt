package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WordTimingTest {

    private fun plainWord(length: Int) = TimingInput(
        coreLength = length,
        endsClause = false,
        endsSentence = false,
        isParagraphEnd = false,
    )

    @Test
    fun `base delay matches 60000 over wpm for a short unpunctuated word`() {
        // coreLength 3 is below the long-word threshold (6), so the length multiplier is 1.0.
        assertEquals(600L, WordTiming.wordDelayMs(100, plainWord(3)))
        assertEquals(200L, WordTiming.wordDelayMs(300, plainWord(3)))
        assertEquals(60L, WordTiming.wordDelayMs(1000, plainWord(3)))
    }

    @Test
    fun `long words take strictly longer than short words at equal wpm`() {
        val short = WordTiming.wordDelayMs(300, plainWord(3))
        val long = WordTiming.wordDelayMs(300, plainWord(12))
        assertTrue(long > short)
    }

    @Test
    fun `sentence pause exceeds clause pause at equal wpm and length`() {
        val clause = WordTiming.wordDelayMs(300, plainWord(4).copy(endsClause = true))
        val sentence = WordTiming.wordDelayMs(300, plainWord(4).copy(endsSentence = true))
        assertTrue(sentence > clause)
    }

    @Test
    fun `paragraph end produces the longest pause`() {
        val sentence = WordTiming.wordDelayMs(300, plainWord(4).copy(endsSentence = true))
        val paragraph = WordTiming.wordDelayMs(300, plainWord(4).copy(isParagraphEnd = true))
        assertTrue(paragraph > sentence)
    }

    @Test
    fun `out-of-range wpm is clamped into 100 to 1000 before the formula runs`() {
        assertEquals(WordTiming.wordDelayMs(100, plainWord(3)), WordTiming.wordDelayMs(50, plainWord(3)))
        assertEquals(WordTiming.wordDelayMs(1000, plainWord(3)), WordTiming.wordDelayMs(5000, plainWord(3)))
    }

    @Test
    fun `result always falls within the configured bounds`() {
        for (wpm in listOf(100, 300, 500, 1000)) {
            for (length in listOf(0, 3, 6, 14, 50)) {
                val delay = WordTiming.wordDelayMs(
                    wpm,
                    TimingInput(length, endsClause = true, endsSentence = true, isParagraphEnd = true),
                )
                assertTrue(delay in 40L..4000L)
            }
        }
    }
}
