package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RsvpTokenizerTest {

    @Test
    fun `token count matches word count for a simple sentence`() {
        val tokens = RsvpTokenizer.tokenize("The quick brown fox jumps.")
        assertEquals(5, tokens.size)
        assertTrue(tokens.last().endsSentence)
    }

    @Test
    fun `only the token before a blank line is marked as a paragraph end`() {
        val text = "First paragraph here.\n\nSecond paragraph starts now."
        val tokens = RsvpTokenizer.tokenize(text)

        val paragraphEnders = tokens.filter { it.isParagraphEnd }
        assertEquals(1, paragraphEnders.size)
        assertEquals("here.", paragraphEnders.first().text)

        // The very last token of the whole text is never a paragraph end.
        assertFalse(tokens.last().isParagraphEnd)
    }

    @Test
    fun `whitespace never produces its own token`() {
        val tokens = RsvpTokenizer.tokenize("word1   word2\tword3\nword4")
        assertEquals(4, tokens.size)
        assertTrue(tokens.none { it.text.isBlank() })
    }

    @Test
    fun `clause punctuation is detected separately from sentence punctuation`() {
        val tokens = RsvpTokenizer.tokenize("First, second; third: fourth.")
        assertTrue(tokens[0].endsClause) // "First,"
        assertFalse(tokens[0].endsSentence)
        assertTrue(tokens.last().endsSentence) // "fourth."
        assertFalse(tokens.last().endsClause)
    }
}
