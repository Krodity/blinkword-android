package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.krodity.blinkword.logic.RsvpToken

/**
 * Renders one RSVP word with its ORP letter fixed at the horizontal center
 * of the row, regardless of word length. Achieved with two equal-weight
 * boxes (right-aligned prefix, left-aligned suffix) around a single fixed
 * highlighted character -- no manual pixel measurement needed.
 *
 * Focus guide marks, when enabled, sit directly above and below that same
 * fixed center column (the outer Column centers both the word row and the
 * marks over the same width, so they always line up with the anchor letter
 * regardless of word length or whether the highlight itself is shown).
 */
@Composable
fun RsvpWordDisplay(
    token: RsvpToken?,
    fontSizeSp: Float,
    wordColor: Color,
    highlightColor: Color,
    showAnchorHighlight: Boolean,
    showFocusGuides: Boolean,
    modifier: Modifier = Modifier,
) {
    if (token == null) return

    val orp = token.orp
    val wordVerticalPadding = if (showFocusGuides) 8.dp else 0.dp

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (showFocusGuides) FocusGuideMark(highlightColor)

        if (orp.orpIndexInOriginal < 0) {
            // Degenerate all-punctuation token: nothing to highlight, just center it.
            Text(
                text = orp.original,
                color = wordColor,
                fontSize = fontSizeSp.sp,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Visible,
                modifier = Modifier.padding(vertical = wordVerticalPadding),
            )
        } else {
            val prefix = orp.original.substring(0, orp.orpIndexInOriginal)
            val highlightChar = orp.original[orp.orpIndexInOriginal].toString()
            val suffix = orp.original.substring(orp.orpIndexInOriginal + 1)

            Row(modifier = Modifier.fillMaxWidth().padding(vertical = wordVerticalPadding)) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = prefix,
                        color = wordColor,
                        fontSize = fontSizeSp.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
                Text(
                    text = highlightChar,
                    color = if (showAnchorHighlight) highlightColor else wordColor,
                    fontWeight = if (showAnchorHighlight) FontWeight.Bold else FontWeight.Normal,
                    fontSize = fontSizeSp.sp,
                    maxLines = 1,
                    softWrap = false,
                )
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = suffix,
                        color = wordColor,
                        fontSize = fontSizeSp.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Visible,
                    )
                }
            }
        }

        if (showFocusGuides) FocusGuideMark(highlightColor)
    }
}

@Composable
private fun FocusGuideMark(color: Color) {
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(14.dp)
            .background(color.copy(alpha = 0.7f)),
    )
}
