package uk.krodity.blinkword.data.tts

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import uk.krodity.blinkword.data.discover.httpGet
import java.io.File
import java.io.IOException
import kotlin.coroutines.coroutineContext

/** Downloads, unpacks and keeps track of installed voice models. */
class NeuralModelRepository(private val context: Context) {

    private val root: File get() = File(context.filesDir, "tts-models")

    fun installedIds(): Set<String> =
        root.listFiles().orEmpty().filter { it.isDirectory && detectModelLayout(it) != null }
            .map { it.name }
            .toSet()

    fun layoutFor(modelId: String): NeuralModelLayout? =
        detectModelLayout(File(root, modelId))

    /**
     * Streams the archive straight through bzip2 and tar into place, so a
     * 100MB model never exists as a temporary file or sits in memory.
     */
    suspend fun install(model: NeuralModel, onProgress: (Float) -> Unit = {}) = withContext(Dispatchers.IO) {
        val target = File(root, model.id)
        if (detectModelLayout(target) != null) return@withContext

        val staging = File(root, "${model.id}.partial")
        staging.deleteRecursively()
        staging.mkdirs()

        try {
            httpGet(model.url) { stream ->
                var readSoFar = 0L
                val counting = object : java.io.FilterInputStream(stream) {
                    override fun read(b: ByteArray, off: Int, len: Int): Int {
                        val count = super.read(b, off, len)
                        if (count > 0) {
                            readSoFar += count
                            onProgress((readSoFar.toFloat() / model.approxBytes).coerceIn(0f, 1f))
                        }
                        return count
                    }
                }

                TarArchiveInputStream(BZip2CompressorInputStream(counting.buffered())).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val destination = File(staging, entry.name).canonicalFile
                        // Refuse paths that would escape the staging directory.
                        if (!destination.path.startsWith(staging.canonicalFile.path)) {
                            throw IOException("Unsafe path in archive: ${entry.name}")
                        }

                        if (entry.isDirectory) {
                            destination.mkdirs()
                        } else {
                            destination.parentFile?.mkdirs()
                            destination.outputStream().use { tar.copyTo(it) }
                        }
                        entry = tar.nextEntry
                    }
                }
            }

            coroutineContext.ensureActive()
            if (detectModelLayout(staging) == null) throw IOException("Downloaded archive had no model in it")

            target.deleteRecursively()
            if (!staging.renameTo(target)) throw IOException("Couldn't move the model into place")
        } catch (e: Throwable) {
            staging.deleteRecursively()
            throw e
        }
    }

    fun delete(modelId: String) {
        File(root, modelId).deleteRecursively()
    }
}
