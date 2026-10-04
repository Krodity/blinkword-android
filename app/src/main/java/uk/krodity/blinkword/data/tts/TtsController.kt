package uk.krodity.blinkword.data.tts

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener

/**
 * Thin wrapper over the platform speech engine.
 *
 * The engine starts asynchronously, so [ensureReady] holds any request until
 * it reports back. [onUtteranceDone] fires only when an utterance finishes on
 * its own -- a [stop] deliberately doesn't, so cancelling never advances the
 * reader past the sentence you were on.
 */
class TtsController(private val context: Context) {

    private var engine: TextToSpeech? = null
    private var ready = false
    private var rate = 1f
    private var voiceName: String? = null

    var onUtteranceDone: ((String) -> Unit)? = null

    fun ensureReady(onResult: (Boolean) -> Unit) {
        if (ready) {
            onResult(true)
            return
        }

        engine?.shutdown()
        engine = TextToSpeech(context) { status ->
            ready = status == TextToSpeech.SUCCESS
            if (ready) {
                engine?.setSpeechRate(rate)
                applyVoice()
                engine?.setOnUtteranceProgressListener(
                    object : UtteranceProgressListener() {
                        override fun onStart(utteranceId: String?) = Unit

                        override fun onDone(utteranceId: String?) {
                            utteranceId?.let { onUtteranceDone?.invoke(it) }
                        }

                        override fun onStop(utteranceId: String?, interrupted: Boolean) = Unit

                        @Deprecated("Required by the base class", ReplaceWith(""))
                        override fun onError(utteranceId: String?) = Unit

                        override fun onError(utteranceId: String?, errorCode: Int) = Unit
                    }
                )
            }
            onResult(ready)
        }
    }

    fun speak(text: String, utteranceId: String) {
        engine?.speak(text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
    }

    fun setRate(value: Float) {
        rate = value.coerceIn(0.5f, 3f)
        engine?.setSpeechRate(rate)
    }

    /** Passing null leaves whichever voice the engine picked by default. */
    fun setVoice(name: String?) {
        voiceName = name
        applyVoice()
    }

    /**
     * Installed voices, device language first and downloadable ones last, so
     * the ones that will actually work offline surface at the top.
     */
    fun availableVoices(): List<TtsVoice> {
        val deviceLanguage = java.util.Locale.getDefault().language

        return runCatching { engine?.voices }.getOrNull().orEmpty()
            .mapNotNull { voice ->
                val locale = runCatching { voice.locale }.getOrNull() ?: return@mapNotNull null
                TtsVoice(
                    name = voice.name,
                    label = "${locale.getDisplayName(java.util.Locale.getDefault())} · " +
                        voiceVariantLabel(voice.name),
                    languageTag = locale.language,
                    requiresNetwork = voice.isNetworkConnectionRequired,
                )
            }
            .sortedWith(
                compareBy(
                    { it.languageTag != deviceLanguage },
                    { it.requiresNetwork },
                    { it.label },
                )
            )
    }

    private fun applyVoice() {
        val active = engine ?: return
        val name = voiceName ?: return
        runCatching { active.voices }.getOrNull()
            ?.firstOrNull { it.name == name }
            ?.let { active.setVoice(it) }
    }

    fun stop() {
        engine?.stop()
    }

    fun shutdown() {
        engine?.stop()
        engine?.shutdown()
        engine = null
        ready = false
    }
}
