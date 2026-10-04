package uk.krodity.blinkword.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import uk.krodity.blinkword.data.AppTheme

private val LightColors = lightColorScheme(
    primary = Color(0xFFD32F2F),
    background = Color(0xFFFDFDFD),
    surface = Color(0xFFFDFDFD),
    onBackground = Color(0xFF1A1A1A),
    onSurface = Color(0xFF1A1A1A),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFFFF6E6E),
    background = Color(0xFF121212),
    surface = Color(0xFF121212),
    onBackground = Color(0xFFECECEC),
    onSurface = Color(0xFFECECEC),
)

private val SepiaColors = lightColorScheme(
    primary = Color(0xFFA13A1E),
    background = Color(0xFFF4ECD8),
    surface = Color(0xFFF4ECD8),
    onBackground = Color(0xFF3B2F1E),
    onSurface = Color(0xFF3B2F1E),
)

/** The word/highlight colors used by the RSVP display -- Material3 has no semantic role for this. */
data class ReaderColors(val word: Color, val highlight: Color)

private val LightReaderColors = ReaderColors(word = Color(0xFF1A1A1A), highlight = Color(0xFFD32F2F))
private val DarkReaderColors = ReaderColors(word = Color(0xFFECECEC), highlight = Color(0xFFFF6E6E))
private val SepiaReaderColors = ReaderColors(word = Color(0xFF3B2F1E), highlight = Color(0xFFA13A1E))

fun readerColorsFor(theme: AppTheme): ReaderColors = when (theme) {
    AppTheme.LIGHT -> LightReaderColors
    AppTheme.DARK -> DarkReaderColors
    AppTheme.SEPIA -> SepiaReaderColors
}

/**
 * Takes an explicit [AppTheme] from settings rather than `isSystemInDarkTheme()`,
 * because Sepia is a three-way user choice, not a light/dark toggle.
 */
@Composable
fun BlinkWordTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val scheme = when (theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.SEPIA -> SepiaColors
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
