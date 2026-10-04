package uk.krodity.blinkword.data.importers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private fun buildEpub(vararg entries: Pair<String, String>): ByteArray {
    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for ((name, content) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

private const val CONTAINER_XML = """<?xml version="1.0"?>
<container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container">
  <rootfiles>
    <rootfile full-path="OEBPS/content.opf" media-type="application/oebps-package+xml"/>
  </rootfiles>
</container>"""

private const val CONTENT_OPF = """<?xml version="1.0"?>
<package xmlns="http://www.idpf.org/2007/opf">
  <metadata>
    <dc:title xmlns:dc="http://purl.org/dc/elements/1.1/">My Book</dc:title>
    <dc:creator xmlns:dc="http://purl.org/dc/elements/1.1/">Jane Author</dc:creator>
  </metadata>
  <manifest>
    <item id="ch1" href="chapter1.xhtml" media-type="application/xhtml+xml"/>
    <item id="ch2" href="chapter2.xhtml" media-type="application/xhtml+xml"/>
  </manifest>
  <spine>
    <itemref idref="ch1"/>
    <itemref idref="ch2"/>
  </spine>
</package>"""

private const val CHAPTER_1 = """<html><body><h1>The Beginning</h1><p>It was a dark night.</p></body></html>"""
private const val CHAPTER_2 = """<html><body><p>No heading here, just text.</p></body></html>"""

class EpubImporterTest {

    @Test
    fun `extracts book title and chapters in spine order`() {
        val epub = buildEpub(
            "META-INF/container.xml" to CONTAINER_XML,
            "OEBPS/content.opf" to CONTENT_OPF,
            "OEBPS/chapter1.xhtml" to CHAPTER_1,
            "OEBPS/chapter2.xhtml" to CHAPTER_2,
        )

        val book = EpubImporter.import(epub.inputStream(), fallbackTitle = "fallback")

        assertEquals("My Book", book.title)
        assertEquals(2, book.chapters.size)
    }

    @Test
    fun `uses the first heading as a chapter title when present`() {
        val epub = buildEpub(
            "META-INF/container.xml" to CONTAINER_XML,
            "OEBPS/content.opf" to CONTENT_OPF,
            "OEBPS/chapter1.xhtml" to CHAPTER_1,
            "OEBPS/chapter2.xhtml" to CHAPTER_2,
        )

        val book = EpubImporter.import(epub.inputStream(), fallbackTitle = "fallback")

        assertEquals("The Beginning", book.chapters[0].title)
        assertTrue(book.chapters[0].text.contains("dark night"))
    }

    @Test
    fun `falls back to a numbered title when a chapter has no heading`() {
        val epub = buildEpub(
            "META-INF/container.xml" to CONTAINER_XML,
            "OEBPS/content.opf" to CONTENT_OPF,
            "OEBPS/chapter1.xhtml" to CHAPTER_1,
            "OEBPS/chapter2.xhtml" to CHAPTER_2,
        )

        val book = EpubImporter.import(epub.inputStream(), fallbackTitle = "fallback")

        assertEquals("Chapter 2", book.chapters[1].title)
        assertTrue(book.chapters[1].text.contains("just text"))
    }

    @Test
    fun `falls back to the file name when the OPF has no title`() {
        val noTitleOpf = CONTENT_OPF.replace("<dc:title xmlns:dc=\"http://purl.org/dc/elements/1.1/\">My Book</dc:title>", "")
        val epub = buildEpub(
            "META-INF/container.xml" to CONTAINER_XML,
            "OEBPS/content.opf" to noTitleOpf,
            "OEBPS/chapter1.xhtml" to CHAPTER_1,
            "OEBPS/chapter2.xhtml" to CHAPTER_2,
        )

        val book = EpubImporter.import(epub.inputStream(), fallbackTitle = "fallback")

        assertEquals("fallback", book.title)
    }

    @Test
    fun `extracts the author from dc-creator`() {
        val epub = buildEpub(
            "META-INF/container.xml" to CONTAINER_XML,
            "OEBPS/content.opf" to CONTENT_OPF,
            "OEBPS/chapter1.xhtml" to CHAPTER_1,
            "OEBPS/chapter2.xhtml" to CHAPTER_2,
        )

        val book = EpubImporter.import(epub.inputStream(), fallbackTitle = "fallback")

        assertEquals("Jane Author", book.author)
    }

    @Test
    fun `throws on a zip that is not a valid epub`() {
        val notAnEpub = buildEpub("hello.txt" to "just a random zip")

        assertThrows(IllegalArgumentException::class.java) {
            EpubImporter.import(notAnEpub.inputStream(), fallbackTitle = "fallback")
        }
    }
}
