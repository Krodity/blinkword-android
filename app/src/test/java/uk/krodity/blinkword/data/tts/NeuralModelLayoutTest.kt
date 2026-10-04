package uk.krodity.blinkword.data.tts

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class NeuralModelLayoutTest {

    @get:Rule
    val temp = TemporaryFolder()

    private fun File.touch(name: String) = File(this, name).apply { parentFile?.mkdirs(); writeText("x") }

    @Test
    fun `reads a kokoro layout`() {
        val dir = temp.newFolder("kokoro-int8-en-v0_19")
        dir.touch("model.int8.onnx")
        dir.touch("tokens.txt")
        dir.touch("voices.bin")
        File(dir, "espeak-ng-data").mkdirs()

        val layout = detectModelLayout(dir)!!

        assertEquals("model.int8.onnx", layout.modelFile.name)
        assertEquals("tokens.txt", layout.tokensFile?.name)
        assertEquals("voices.bin", layout.voicesFile?.name)
        assertEquals("espeak-ng-data", layout.dataDir?.name)
        assertTrue(layout.isKokoro)
    }

    @Test
    fun `reads a piper vits layout`() {
        val dir = temp.newFolder("vits-piper-en_US-amy-low")
        dir.touch("en_US-amy-low.onnx")
        dir.touch("tokens.txt")
        File(dir, "espeak-ng-data").mkdirs()

        val layout = detectModelLayout(dir)!!

        assertEquals("en_US-amy-low.onnx", layout.modelFile.name)
        assertNull(layout.voicesFile)
        assertTrue(!layout.isKokoro)
    }

    @Test
    fun `descends into the folder the archive unpacked into`() {
        val outer = temp.newFolder("install")
        val inner = File(outer, "vits-piper-en_US-amy-low").apply { mkdirs() }
        inner.touch("en_US-amy-low.onnx")
        inner.touch("tokens.txt")

        val layout = detectModelLayout(outer)!!

        assertEquals("en_US-amy-low.onnx", layout.modelFile.name)
    }

    @Test
    fun `prefers the quantised model when both are present`() {
        val dir = temp.newFolder("kokoro")
        dir.touch("model.onnx")
        dir.touch("model.int8.onnx")

        assertEquals("model.int8.onnx", detectModelLayout(dir)!!.modelFile.name)
    }

    @Test
    fun `picks up optional lexicon and dict entries`() {
        val dir = temp.newFolder("kokoro-multi")
        dir.touch("model.onnx")
        dir.touch("lexicon-us-en.txt")
        dir.touch("lexicon-zh.txt")
        File(dir, "dict").mkdirs()

        val layout = detectModelLayout(dir)!!

        assertEquals(2, layout.lexiconFiles.size)
        assertEquals("dict", layout.dictDir?.name)
    }

    @Test
    fun `a directory with no model is not a layout`() {
        assertNull(detectModelLayout(temp.newFolder("empty")))
    }
}
