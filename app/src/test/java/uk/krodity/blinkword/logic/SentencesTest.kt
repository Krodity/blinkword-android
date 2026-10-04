package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Test

private fun tokens(text: String): List<RsvpToken> = RsvpTokenizer.tokenize(text)

class SentencesTest {

    @Test
    fun `splits on sentence endings`() {
        val ranges = sentenceRanges(tokens("One two three. Four five. Six."))

        assertEquals(listOf(0..2, 3..4, 5..5), ranges)
    }

    @Test
    fun `splits on paragraph breaks even without punctuation`() {
        val ranges = sentenceRanges(tokens("first para words\n\nsecond para words"))

        assertEquals(2, ranges.size)
        assertEquals(0..2, ranges.first())
    }

    @Test
    fun `caps runaway text with no punctuation at all`() {
        val ranges = sentenceRanges(tokens(List(25) { "word" }.joinToString(" ")), maxTokens = 10)

        assertEquals(listOf(0..9, 10..19, 20..24), ranges)
    }

    @Test
    fun `trailing text without a full stop still becomes a chunk`() {
        // "Done." | "and" "then" "more"
        val ranges = sentenceRanges(tokens("Done. and then more"))

        assertEquals(listOf(0..0, 1..3), ranges)
    }

    @Test
    fun `empty input has no chunks`() {
        assertEquals(emptyList<IntRange>(), sentenceRanges(emptyList()))
        assertEquals(-1, sentenceIndexFor(emptyList(), 0))
    }

    @Test
    fun `finds the chunk holding a word`() {
        val ranges = listOf(0..2, 3..4, 5..9)

        assertEquals(0, sentenceIndexFor(ranges, 1))
        assertEquals(1, sentenceIndexFor(ranges, 3))
        assertEquals(2, sentenceIndexFor(ranges, 9))
    }

    @Test
    fun `an index past the end resolves to the last chunk`() {
        assertEquals(2, sentenceIndexFor(listOf(0..2, 3..4, 5..9), 999))
    }
}
