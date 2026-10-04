package uk.krodity.blinkword.logic

/** One RSVP-displayable word: its ORP breakdown plus the pause signals that follow it. */
data class RsvpToken(
    val text: String,
    val orp: OrpWord,
    val endsClause: Boolean,
    val endsSentence: Boolean,
    val isParagraphEnd: Boolean,
) {
    fun toTimingInput(): TimingInput = TimingInput(
        coreLength = orp.core.length,
        endsClause = endsClause,
        endsSentence = endsSentence,
        isParagraphEnd = isParagraphEnd,
    )
}

private val PARAGRAPH_BREAK = Regex("\\n\\s*\\n")
private val WHITESPACE = Regex("\\s+")
private val SENTENCE_ENDERS = charArrayOf('.', '!', '?', '…')
private val CLAUSE_ENDERS = charArrayOf(',', ';', ':', '—', '-')

object RsvpTokenizer {

    /**
     * Splits raw text into paragraphs (blank-line separated), then into
     * whitespace-separated words, tagging each with the punctuation/paragraph
     * signals [WordTiming] needs. This is also what determines a document's
     * word count, so counting and playback always agree.
     */
    fun tokenize(rawText: String): List<RsvpToken> {
        val paragraphs = rawText.split(PARAGRAPH_BREAK).map { it.trim() }.filter { it.isNotEmpty() }
        val tokens = mutableListOf<RsvpToken>()

        paragraphs.forEachIndexed { paragraphIndex, paragraph ->
            val words = paragraph.split(WHITESPACE).filter { it.isNotEmpty() }
            val isLastParagraph = paragraphIndex == paragraphs.lastIndex

            words.forEachIndexed { wordIndex, word ->
                val lastChar = word.trimEnd().lastOrNull()
                val endsSentence = lastChar != null && lastChar in SENTENCE_ENDERS
                val endsClause = !endsSentence && lastChar != null && lastChar in CLAUSE_ENDERS
                val isParagraphEnd = !isLastParagraph && wordIndex == words.lastIndex

                tokens += RsvpToken(
                    text = word,
                    orp = OrpCalculator.computeOrp(word),
                    endsClause = endsClause,
                    endsSentence = endsSentence,
                    isParagraphEnd = isParagraphEnd,
                )
            }
        }

        return tokens
    }
}
