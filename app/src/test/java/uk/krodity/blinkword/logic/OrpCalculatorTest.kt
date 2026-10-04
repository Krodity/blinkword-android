package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Test

class OrpCalculatorTest {

    @Test
    fun `bucket boundaries match the standard RSVP table`() {
        assertEquals(0, OrpCalculator.orpIndexForLength(1))
        assertEquals(1, OrpCalculator.orpIndexForLength(2))
        assertEquals(1, OrpCalculator.orpIndexForLength(5))
        assertEquals(2, OrpCalculator.orpIndexForLength(6))
        assertEquals(2, OrpCalculator.orpIndexForLength(9))
        assertEquals(3, OrpCalculator.orpIndexForLength(10))
        assertEquals(3, OrpCalculator.orpIndexForLength(13))
        assertEquals(4, OrpCalculator.orpIndexForLength(14))
        assertEquals(4, OrpCalculator.orpIndexForLength(20))
    }

    @Test
    fun `plain word highlights the correct letter`() {
        // "cat" -> core length 3 -> bucket 1 -> index 1 -> 'a'
        val word = OrpCalculator.computeOrp("cat")
        assertEquals(1, word.orpIndexInOriginal)
        assertEquals('a', word.original[word.orpIndexInOriginal])
    }

    @Test
    fun `leading punctuation offsets the highlight index`() {
        // "\"hello" -> leading quote, core "hello" (len 5, bucket 1) -> highlight at leadingEnd(1)+1 = 2
        val word = OrpCalculator.computeOrp("\"hello")
        assertEquals("\"", word.leading)
        assertEquals("hello", word.core)
        assertEquals(2, word.orpIndexInOriginal)
        assertEquals('e', word.original[word.orpIndexInOriginal])
    }

    @Test
    fun `trailing punctuation does not affect the highlight index`() {
        val bare = OrpCalculator.computeOrp("word")
        val punctuated = OrpCalculator.computeOrp("word.")
        assertEquals(bare.orpIndexInOriginal, punctuated.orpIndexInOriginal)
        assertEquals("word", punctuated.core)
        assertEquals(".", punctuated.trailing)
    }

    @Test
    fun `punctuation on both sides is stripped from core only`() {
        val word = OrpCalculator.computeOrp("(quoted),")
        assertEquals("(", word.leading)
        assertEquals("quoted", word.core)
        assertEquals("),", word.trailing)
    }

    @Test
    fun `all-punctuation token has no highlight`() {
        val word = OrpCalculator.computeOrp("--")
        assertEquals("", word.core)
        assertEquals(-1, word.orpIndexInOriginal)
    }

    @Test
    fun `internal punctuation is not stripped from the core`() {
        val apostrophe = OrpCalculator.computeOrp("don't")
        assertEquals("don't", apostrophe.core)

        val hyphen = OrpCalculator.computeOrp("well-known")
        assertEquals("well-known", hyphen.core)
    }
}
