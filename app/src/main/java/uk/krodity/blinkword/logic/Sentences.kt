package uk.krodity.blinkword.logic

/**
 * Longest run of words handed to the speech engine in one go. Engines cap
 * their input length, and a shorter utterance also means reading position
 * advances (and is saved) more often, so stopping mid-book loses less.
 */
const val MAX_SPOKEN_TOKENS = 60

/**
 * Splits the token stream into the chunks text-to-speech reads one at a time:
 * sentences, also broken at paragraph ends and at [maxTokens] so that text
 * without punctuation can't produce one enormous utterance.
 */
fun sentenceRanges(tokens: List<RsvpToken>, maxTokens: Int = MAX_SPOKEN_TOKENS): List<IntRange> {
    if (tokens.isEmpty()) return emptyList()

    val ranges = mutableListOf<IntRange>()
    var start = 0

    tokens.forEachIndexed { index, token ->
        val longEnough = index - start + 1 >= maxTokens
        if (token.endsSentence || token.isParagraphEnd || longEnough) {
            ranges += start..index
            start = index + 1
        }
    }
    if (start <= tokens.lastIndex) ranges += start..tokens.lastIndex

    return ranges
}

/** Which chunk holds [tokenIndex]; the nearest preceding one if it falls in a gap. */
fun sentenceIndexFor(ranges: List<IntRange>, tokenIndex: Int): Int {
    if (ranges.isEmpty()) return -1

    val exact = ranges.indexOfFirst { tokenIndex in it }
    if (exact >= 0) return exact

    return if (tokenIndex < ranges.first().first) 0 else ranges.lastIndex
}
