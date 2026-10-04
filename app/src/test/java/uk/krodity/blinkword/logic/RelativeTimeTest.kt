package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit

class RelativeTimeTest {

    private val now = 1_700_000_000_000L

    private fun ago(amount: Long, unit: TimeUnit) = now - unit.toMillis(amount)

    @Test
    fun `under a minute reads just now`() {
        assertEquals("just now", relativeTime(ago(30, TimeUnit.SECONDS), now))
    }

    @Test
    fun `minutes ago`() {
        assertEquals("4 min ago", relativeTime(ago(4, TimeUnit.MINUTES), now))
    }

    @Test
    fun `hours ago`() {
        assertEquals("5 hr ago", relativeTime(ago(5, TimeUnit.HOURS), now))
    }

    @Test
    fun `days ago`() {
        assertEquals("6 days ago", relativeTime(ago(6, TimeUnit.DAYS), now))
    }

    @Test
    fun `weeks ago`() {
        assertEquals("2 wk ago", relativeTime(ago(15, TimeUnit.DAYS), now))
    }

    @Test
    fun `months ago`() {
        assertEquals("2 mo ago", relativeTime(ago(65, TimeUnit.DAYS), now))
    }

    @Test
    fun `years ago`() {
        assertEquals("2 yr ago", relativeTime(ago(800, TimeUnit.DAYS), now))
    }
}
