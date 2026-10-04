package uk.krodity.blinkword.data.tts

import java.io.File

/**
 * Where the pieces of an unpacked voice model live.
 *
 * Filenames differ between model families and quantisations -- `model.onnx`,
 * `model.int8.onnx`, `en_US-amy-low.onnx` -- so they're discovered by
 * inspecting the extracted directory rather than hardcoded per model. Getting
 * this wrong only shows up at synthesis time, so it's kept pure and tested.
 */
data class NeuralModelLayout(
    val modelFile: File,
    val tokensFile: File?,
    val voicesFile: File?,
    val dataDir: File?,
    val dictDir: File?,
    val lexiconFiles: List<File>,
) {
    /** Kokoro carries a `voices.bin` holding its speaker embeddings; VITS doesn't. */
    val isKokoro: Boolean get() = voicesFile != null
}

fun detectModelLayout(directory: File): NeuralModelLayout? {
    // Archives unpack into a single top-level folder; follow it down.
    val root = directory.takeIf { it.containsModel() }
        ?: directory.listFiles()?.firstOrNull { it.isDirectory && it.containsModel() }
        ?: return null

    val files = root.listFiles().orEmpty()

    // Prefer the quantised model when a directory ships more than one.
    val model = files.filter { it.isFile && it.name.endsWith(".onnx") }
        .minByOrNull { if (it.name.contains("int8")) 0 else 1 }
        ?: return null

    return NeuralModelLayout(
        modelFile = model,
        tokensFile = files.firstOrNull { it.name == "tokens.txt" },
        voicesFile = files.firstOrNull { it.name == "voices.bin" },
        dataDir = files.firstOrNull { it.isDirectory && it.name == "espeak-ng-data" },
        dictDir = files.firstOrNull { it.isDirectory && it.name.endsWith("dict") },
        lexiconFiles = files.filter { it.isFile && it.name.startsWith("lexicon") && it.name.endsWith(".txt") },
    )
}

private fun File.containsModel(): Boolean =
    isDirectory && listFiles().orEmpty().any { it.isFile && it.name.endsWith(".onnx") }
