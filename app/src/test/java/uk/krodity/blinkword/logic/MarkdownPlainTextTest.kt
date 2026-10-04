package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownPlainTextTest {

    @Test
    fun `strips heading markers`() {
        assertEquals("Title", MarkdownPlainText.toPlainText("# Title").trim())
        assertEquals("Subtitle", MarkdownPlainText.toPlainText("### Subtitle").trim())
    }

    @Test
    fun `unwraps bold and italic emphasis`() {
        assertEquals("bold word", MarkdownPlainText.toPlainText("**bold word**").trim())
        assertEquals("italic word", MarkdownPlainText.toPlainText("_italic word_").trim())
        assertEquals("both", MarkdownPlainText.toPlainText("***both***").trim())
    }

    @Test
    fun `turns links into their visible text`() {
        val result = MarkdownPlainText.toPlainText("See [the docs](https://example.com/docs) for more.")
        assertEquals("See the docs for more.", result)
    }

    @Test
    fun `drops images but keeps alt text`() {
        val result = MarkdownPlainText.toPlainText("![a red fox](fox.png) is fast")
        assertEquals("a red fox is fast", result)
    }

    @Test
    fun `strips list markers`() {
        val result = MarkdownPlainText.toPlainText("- first\n- second\n1. third")
        assertFalse(result.contains("-"))
        assertTrue(result.contains("first"))
        assertTrue(result.contains("third"))
    }

    @Test
    fun `strips code fences and inline code`() {
        val fenced = MarkdownPlainText.toPlainText("```\nval x = 1\n```")
        assertFalse(fenced.contains("```"))

        val inline = MarkdownPlainText.toPlainText("Use `foo()` here").trim()
        assertEquals("Use foo() here", inline)
    }
}
