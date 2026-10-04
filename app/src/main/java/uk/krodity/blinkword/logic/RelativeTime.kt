package uk.krodity.blinkword.logic

import java.util.concurrent.TimeUnit

/** Formats a past timestamp as "4 min ago" / "6 days ago" style relative text. */
fun relativeTime(epochMillis: Long, nowMillis: Long = System.currentTimeMillis()): String {
    val diff = (nowMillis - epochMillis).coerceAtLeast(0)

    val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
    val hours = TimeUnit.MILLISECONDS.toHours(diff)
    val days = TimeUnit.MILLISECONDS.toDays(diff)

    return when {
        minutes < 1 -> "just now"
        minutes < 60 -> "$minutes min ago"
        hours < 24 -> "$hours hr ago"
        days < 7 -> "$days days ago"
        days < 30 -> "${days / 7} wk ago"
        days < 365 -> "${days / 30} mo ago"
        else -> "${days / 365} yr ago"
    }
}
