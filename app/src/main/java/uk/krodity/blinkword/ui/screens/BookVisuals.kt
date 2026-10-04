package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/**
 * Shared book-cover and format-badge visuals. Documents carry no cover art, so
 * a cover is generated from the title -- deterministically, so the same book
 * always looks the same in the library, in a grid tile, and in Discover.
 */
internal fun bookColor(title: String): Color {
    val hue = ((title.hashCode() % 360) + 360) % 360
    return Color.hsv(hue.toFloat(), 0.45f, 0.82f)
}

@Composable
internal fun BookThumbnail(title: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(RoundedCornerShape(8.dp)).background(bookColor(title)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = title.trim().take(1).uppercase().ifEmpty { "?" },
            color = Color.White,
            style = MaterialTheme.typography.titleLarge,
        )
    }
}

/** A short badge label for a stored [uk.krodity.blinkword.data.importers.DocumentFormat] name. */
internal fun formatLabel(format: String): String = when (format) {
    "EPUB" -> "ePub"
    "PDF" -> "PDF"
    "MARKDOWN" -> "MD"
    "PLAIN_TEXT" -> "TXT"
    "TEXT" -> "Pasted"
    else -> format
}

internal fun formatBadgeColor(format: String): Color = when (format) {
    "EPUB" -> Color(0xFF2E7D32)
    "PDF" -> Color(0xFF1565C0)
    "MARKDOWN" -> Color(0xFF6A1B9A)
    else -> Color(0xFF616161)
}

@Composable
internal fun FormatBadge(format: String) {
    val color = formatBadgeColor(format)
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text = formatLabel(format), style = MaterialTheme.typography.labelSmall, color = color)
    }
}
