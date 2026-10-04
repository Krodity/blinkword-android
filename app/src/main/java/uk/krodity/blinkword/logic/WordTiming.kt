package uk.krodity.blinkword.logic

import kotlin.math.roundToLong

/** The signals [WordTiming.wordDelayMs] needs to time a single word's display. */
data class TimingInput(
    val coreLength: Int,
    val endsClause: Boolean,
    val endsSentence: Boolean,
    val isParagraphEnd: Boolean,
)

/**
 * Per-word RSVP display duration. Base pace comes straight from WPM;
 * long words and punctuation add pauses expressed as *fractions of base*
 * (not fixed millisecond constants) so they scale naturally with speed
 * instead of feeling disproportionate at 1000 wpm vs 100 wpm.
 */
object WordTiming {
    private const val LONG_WORD_THRESHOLD = 6
    private const val MAX_LENGTH_MULTIPLIER = 2.5
    private const val LENGTH_STEP_FACTOR = 0.06
    private const val CLAUSE_PAUSE_FACTOR = 0.3
    private const val SENTENCE_PAUSE_FACTOR = 0.6
    private const val PARAGRAPH_PAUSE_FACTOR = 0.8
    private const val MIN_DELAY_MS = 40L
    private const val MAX_DELAY_MS = 4000L

    fun wordDelayMs(wpm: Int, input: TimingInput): Long {
        val clampedWpm = wpm.coerceIn(100, 1000)
        val base = 60_000.0 / clampedWpm

        val lengthMultiplier = (1.0 + (input.coreLength - LONG_WORD_THRESHOLD).coerceAtLeast(0) * LENGTH_STEP_FACTOR)
            .coerceAtMost(MAX_LENGTH_MULTIPLIER)

        var delay = base * lengthMultiplier
        if (input.endsClause) delay += base * CLAUSE_PAUSE_FACTOR
        if (input.endsSentence) delay += base * SENTENCE_PAUSE_FACTOR
        if (input.isParagraphEnd) delay += base * PARAGRAPH_PAUSE_FACTOR

        return delay.roundToLong().coerceIn(MIN_DELAY_MS, MAX_DELAY_MS)
    }
}
