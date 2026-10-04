package uk.krodity.blinkword.data.tts

private const val MODELS = "https://github.com/k2-fsa/sherpa-onnx/releases/download/tts-models"

/** A downloadable neural voice model. */
data class NeuralModel(
    val id: String,
    val displayName: String,
    val description: String,
    val url: String,
    val approxBytes: Long,
    /**
     * Speaker names in model order. Kokoro packs many voices into one file;
     * Piper and Matcha models hold a single voice. Empty means "ask the model
     * how many it has and number them".
     */
    val speakers: List<String> = emptyList(),
)

/**
 * The models offered in Settings: a spread of architectures and voices rather
 * than the whole upstream catalogue, so there's something to switch to when
 * one of them doesn't suit.
 *
 * Quantisation is called out in the descriptions because it costs real audio
 * quality -- fp16 is close to indistinguishable from full precision, int8 is
 * audibly rougher, and that tradeoff is the point of having a list.
 */
object NeuralVoiceCatalogue {

    private val KOKORO_V019_VOICES = listOf(
        "Alloy (US, female)",
        "Bella (US, female)",
        "Nicole (US, female)",
        "Sarah (US, female)",
        "Sky (US, female)",
        "Adam (US, male)",
        "Michael (US, male)",
        "Emma (UK, female)",
        "Isabella (UK, female)",
        "George (UK, male)",
        "Lewis (UK, male)",
    )

    val models = listOf(
        NeuralModel(
            id = "matcha-icefall-en_US-ljspeech",
            displayName = "Matcha · LJSpeech",
            description = "US female. Different architecture to the others -- try this first",
            url = "$MODELS/matcha-icefall-en_US-ljspeech.tar.bz2",
            approxBytes = 77_000_000,
        ),
        NeuralModel(
            id = "vits-piper-en_US-lessac-high-fp16",
            displayName = "Piper · Lessac (high)",
            description = "US female, clear narration voice, near-full quality",
            url = "$MODELS/vits-piper-en_US-lessac-high-fp16.tar.bz2",
            approxBytes = 58_000_000,
        ),
        NeuralModel(
            id = "vits-piper-en_US-ryan-high-fp16",
            displayName = "Piper · Ryan (high)",
            description = "US male, near-full quality",
            url = "$MODELS/vits-piper-en_US-ryan-high-fp16.tar.bz2",
            approxBytes = 59_000_000,
        ),
        NeuralModel(
            id = "vits-piper-en_GB-alan-medium-fp16",
            displayName = "Piper · Alan (UK)",
            description = "UK male",
            url = "$MODELS/vits-piper-en_GB-alan-medium-fp16.tar.bz2",
            approxBytes = 36_000_000,
        ),
        NeuralModel(
            id = "kokoro-multi-lang-v1_1",
            displayName = "Kokoro v1.1 (many voices)",
            description = "Newer Kokoro at full precision -- big, but not the compressed one",
            url = "$MODELS/kokoro-multi-lang-v1_1.tar.bz2",
            approxBytes = 365_000_000,
        ),
        NeuralModel(
            id = "kokoro-int8-en-v0_19",
            displayName = "Kokoro v0.19 (compressed)",
            description = "11 voices, int8-compressed -- smallest Kokoro, roughest sounding",
            url = "$MODELS/kokoro-int8-en-v0_19.tar.bz2",
            approxBytes = 103_200_000,
            speakers = KOKORO_V019_VOICES,
        ),
        NeuralModel(
            id = "vits-piper-en_US-amy-low-int8",
            displayName = "Piper · Amy (tiny)",
            description = "US female, smallest and fastest download",
            url = "$MODELS/vits-piper-en_US-amy-low-int8.tar.bz2",
            approxBytes = 21_100_000,
        ),
    )

    fun find(id: String): NeuralModel? = models.firstOrNull { it.id == id }

    /**
     * Labels for a model's speakers. Falls back to numbering when the model
     * reports a different count than the catalogue expects, so a mismatch
     * shows honest names rather than wrong ones.
     */
    fun speakerLabels(model: NeuralModel, reportedCount: Int): List<String> = when {
        reportedCount <= 1 -> listOf(model.displayName)
        model.speakers.size == reportedCount -> model.speakers
        else -> (1..reportedCount).map { "Voice $it" }
    }
}

/** Spoken by the preview button, with enough variety to judge a voice by. */
const val PREVIEW_SENTENCE =
    "This is how your books will sound. The quick brown fox jumps over the lazy dog."
