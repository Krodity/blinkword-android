package uk.krodity.blinkword.data.tts

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import com.k2fsa.sherpa.onnx.OfflineTts
import com.k2fsa.sherpa.onnx.OfflineTtsConfig
import com.k2fsa.sherpa.onnx.OfflineTtsKokoroModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsModelConfig
import com.k2fsa.sherpa.onnx.OfflineTtsVitsModelConfig
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Offline neural speech via sherpa-onnx.
 *
 * Audio is written to [AudioTrack] as it is generated rather than after the
 * whole sentence is synthesised -- on a phone a long sentence takes a couple
 * of seconds to render, and waiting for all of it before making a sound would
 * be audible as a stutter between sentences.
 */
class NeuralTtsEngine {

    private var tts: OfflineTts? = null
    private var loadedModelId: String? = null
    private var track: AudioTrack? = null
    private val cancelled = AtomicBoolean(false)

    val speakerCount: Int get() = tts?.numSpeakers() ?: 0

    @Synchronized
    fun load(modelId: String, layout: NeuralModelLayout) {
        if (loadedModelId == modelId && tts != null) return

        release()

        val config = OfflineTtsConfig(
            model = OfflineTtsModelConfig(
                vits = if (layout.isKokoro) {
                    OfflineTtsVitsModelConfig()
                } else {
                    OfflineTtsVitsModelConfig(
                        model = layout.modelFile.absolutePath,
                        tokens = layout.tokensFile?.absolutePath.orEmpty(),
                        dataDir = layout.dataDir?.absolutePath.orEmpty(),
                        dictDir = layout.dictDir?.absolutePath.orEmpty(),
                        lexicon = layout.lexiconFiles.joinToString(",") { it.absolutePath },
                    )
                },
                kokoro = if (layout.isKokoro) {
                    OfflineTtsKokoroModelConfig(
                        model = layout.modelFile.absolutePath,
                        voices = layout.voicesFile?.absolutePath.orEmpty(),
                        tokens = layout.tokensFile?.absolutePath.orEmpty(),
                        dataDir = layout.dataDir?.absolutePath.orEmpty(),
                        dictDir = layout.dictDir?.absolutePath.orEmpty(),
                        lexicon = layout.lexiconFiles.joinToString(",") { it.absolutePath },
                    )
                } else {
                    OfflineTtsKokoroModelConfig()
                },
                numThreads = 2,
            ),
        )

        tts = OfflineTts(assetManager = null, config = config)
        loadedModelId = modelId
    }

    /**
     * Synthesises and plays [text], blocking until it finishes or [stop] is
     * called. Returns false when it was cut short.
     */
    fun speakBlocking(text: String, speakerId: Int, speed: Float): Boolean {
        val engine = tts ?: return false
        cancelled.set(false)

        val sampleRate = engine.sampleRate()
        val player = openTrack(sampleRate)
        track = player
        player.play()

        // Must be an explicit object, not a lambda. sherpa-onnx's JNI resolves
        // this callback by exact signature -- ([F)Ljava/lang/Integer; -- and a
        // Kotlin lambda compiles to an invokedynamic-desugared class carrying
        // only the erased invoke(Object)Object bridge, which the native lookup
        // misses and then aborts the whole process on.
        val sink = object : Function1<FloatArray, Int> {
            override fun invoke(samples: FloatArray): Int {
                if (cancelled.get()) return 0 // anything but 1 abandons generation
                player.write(samples, 0, samples.size, AudioTrack.WRITE_BLOCKING)
                return 1
            }
        }

        engine.generateWithCallback(text = text, sid = speakerId, speed = speed, callback = sink)

        val finished = !cancelled.get()
        if (finished) {
            // Let the queued tail actually play out before tearing the track down.
            player.stop()
        } else {
            player.pause()
            player.flush()
        }
        player.release()
        if (track === player) track = null

        return finished
    }

    fun stop() {
        cancelled.set(true)
        runCatching {
            track?.pause()
            track?.flush()
        }
    }

    @Synchronized
    fun release() {
        stop()
        runCatching { tts?.release() }
        tts = null
        loadedModelId = null
    }

    private fun openTrack(sampleRate: Int): AudioTrack {
        val bufferSize = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_FLOAT,
        ).coerceAtLeast(sampleRate) // at least a second of headroom

        return AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_FLOAT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }
}

/** True when the model directory holds everything the engine needs. */
fun File.hasNeuralModel(): Boolean = detectModelLayout(this) != null
