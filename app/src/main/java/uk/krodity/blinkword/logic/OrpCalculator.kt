package uk.krodity.blinkword.logic

/**
 * A tokenized word split into the parts needed to render RSVP with an
 * Optimal Recognition Point (ORP) highlight.
 *
 * [orpIndexInOriginal] indexes into [original] directly (not into [core]),
 * so the renderer can slice the real string without re-deriving offsets.
 * It is -1 for a degenerate token with no letters/digits at all (e.g. a
 * lone "--"), which the renderer should render centered and unhighlighted.
 */
data class OrpWord(
    val original: String,
    val leading: String,
    val core: String,
    val trailing: String,
    val orpIndexInOriginal: Int,
)

object OrpCalculator {

    /**
     * Strips only leading/trailing non-alphanumeric runs (internal
     * punctuation like "don't" or "well-known" stays in [OrpWord.core]),
     * then picks the ORP letter from the standard RSVP bucket table on the
     * letter-only core length.
     */
    fun computeOrp(token: String): OrpWord {
        val leadingEnd = token.indexOfFirst { it.isLetterOrDigit() }
            .let { if (it == -1) token.length else it }
        val trailingStart = token.indexOfLast { it.isLetterOrDigit() }
            .let { if (it == -1) token.length else it + 1 }

        val leading = token.substring(0, leadingEnd)
        val core = if (leadingEnd < trailingStart) token.substring(leadingEnd, trailingStart) else ""
        val trailing = token.substring(trailingStart)

        val orpIndex = if (core.isEmpty()) -1 else leadingEnd + orpIndexForLength(core.length)
        return OrpWord(token, leading, core, trailing, orpIndex)
    }

    /** 1->0, 2..5->1, 6..9->2, 10..13->3, 14+->4 -- standard RSVP ORP bucket table. */
    fun orpIndexForLength(length: Int): Int = when {
        length <= 1 -> 0
        length in 2..5 -> 1
        length in 6..9 -> 2
        length in 10..13 -> 3
        else -> 4
    }
}
