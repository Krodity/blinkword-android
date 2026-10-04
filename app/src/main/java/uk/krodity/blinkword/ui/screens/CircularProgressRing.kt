package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

/** A ring showing [progress] (0f..1f) with the percentage as a plain number in the center. */
@Composable
fun CircularProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    strokeWidth: Dp = 4.dp,
    color: Color = MaterialTheme.colorScheme.primary,
    trackColor: Color = MaterialTheme.colorScheme.outlineVariant,
) {
    val clamped = progress.coerceIn(0f, 1f)
    Box(modifier = modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(color = trackColor, startAngle = -90f, sweepAngle = 360f, useCenter = false, style = stroke)
            drawArc(color = color, startAngle = -90f, sweepAngle = 360f * clamped, useCenter = false, style = stroke)
        }
        Text(text = "${(clamped * 100).roundToInt()}", style = MaterialTheme.typography.labelLarge)
    }
}
