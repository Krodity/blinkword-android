package uk.krodity.blinkword.data.tts

/** An installed speech voice, described well enough to pick one from a list. */
data class TtsVoice(
    /** The engine's own identifier, e.g. `en-us-x-sfg#male_1-local`. */
    val name: String,
    val label: String,
    val languageTag: String,
    val requiresNetwork: Boolean,
)

/**
 * Turns an engine voice name into something readable.
 *
 * Names follow no standard, but most engines tack a variant onto the end after
 * a `#` -- `en-us-x-sfg#male_1-local` -- which is the only part that
 * distinguishes two voices of the same language. Anything without one falls
 * back to the trailing segment of the name.
 */
fun voiceVariantLabel(voiceName: String): String {
    val variant = voiceName.substringAfter('#', "")
        .removeSuffix("-local")
        .removeSuffix("-network")
        .replace('_', ' ')
        .replace('-', ' ')
        .trim()

    if (variant.isNotEmpty()) return variant.replaceFirstChar { it.uppercase() }

    return voiceName.substringAfterLast('-', voiceName)
        .replace('_', ' ')
        .trim()
        .replaceFirstChar { it.uppercase() }
}
