package uk.krodity.blinkword.data.importers

import org.jsoup.Jsoup
import org.jsoup.parser.Parser
import java.io.InputStream
import java.util.zip.ZipInputStream

/**
 * Parses an ePub (a zip of XHTML chapters described by an OPF manifest/spine)
 * into plain text per chapter. Chapter titles come from the first heading in
 * each chapter's XHTML, since mapping the NCX/nav table of contents to spine
 * entries is unreliable across the format's many real-world variations.
 */
object EpubImporter {

    fun import(input: InputStream, fallbackTitle: String): ImportedBook {
        val entries = readZipEntries(input)

        val containerXml = entries["META-INF/container.xml"]?.decodeToString()
            ?: throw IllegalArgumentException("Not a valid ePub: missing container.xml")
        val container = Jsoup.parse(containerXml, "", Parser.xmlParser())
        val opfPath = container.select("rootfile").firstOrNull()?.attr("full-path")
            ?: throw IllegalArgumentException("Not a valid ePub: no rootfile in container.xml")

        val opfBytes = entries[opfPath]
            ?: throw IllegalArgumentException("Not a valid ePub: missing $opfPath")
        val opf = Jsoup.parse(opfBytes.decodeToString(), "", Parser.xmlParser())

        val basePath = opfPath.substringBeforeLast('/', "")
        val idToHref = opf.select("manifest > item").associate { it.attr("id") to it.attr("href") }
        val spineIds = opf.select("spine > itemref").map { it.attr("idref") }

        // OPF metadata titles/authors are namespaced (<dc:title>, <dc:creator>),
        // so match by local name rather than a CSS tag selector, which only
        // matches the exact tag name.
        val metadataChildren = opf.select("metadata").firstOrNull()?.children().orEmpty()
        val bookTitle = metadataChildren
            .firstOrNull { it.tagName().substringAfter(':').equals("title", ignoreCase = true) }
            ?.text()
            ?.takeIf { it.isNotBlank() }
            ?: fallbackTitle
        val author = metadataChildren
            .firstOrNull { it.tagName().substringAfter(':').equals("creator", ignoreCase = true) }
            ?.text()
            ?.takeIf { it.isNotBlank() }

        val chapters = spineIds.mapIndexedNotNull { index, idref ->
            val href = idToHref[idref] ?: return@mapIndexedNotNull null
            val path = normalizePath(if (basePath.isEmpty()) href else "$basePath/$href")
            val bytes = entries[path] ?: return@mapIndexedNotNull null

            val chapterHtml = Jsoup.parse(bytes.decodeToString())
            val text = chapterHtml.body().text()
            if (text.isBlank()) return@mapIndexedNotNull null

            val heading = chapterHtml.select("h1, h2, h3").firstOrNull()?.text()?.takeIf { it.isNotBlank() }
            ImportedChapter(title = heading ?: "Chapter ${index + 1}", text = text)
        }

        if (chapters.isEmpty()) throw IllegalArgumentException("ePub had no readable chapters")
        return ImportedBook(bookTitle, chapters, author, findCover(opf, entries, basePath))
    }

    /**
     * Digs the cover image out of the archive. ePubs mark it three different
     * ways depending on which version and toolchain produced them, so all three
     * are tried before giving up: the epub3 `properties="cover-image"`, the
     * epub2 `<meta name="cover">` pointer, then a plain name match.
     */
    private fun findCover(
        opf: org.jsoup.nodes.Document,
        entries: Map<String, ByteArray>,
        basePath: String,
    ): ByteArray? {
        val items = opf.select("manifest > item")

        val epub3 = items.firstOrNull { it.attr("properties").contains("cover-image") }

        val metaCoverId = opf.select("metadata > meta")
            .firstOrNull { it.attr("name").equals("cover", ignoreCase = true) }
            ?.attr("content")
        val epub2 = metaCoverId?.let { id -> items.firstOrNull { it.attr("id") == id } }

        val byName = items.firstOrNull {
            it.attr("media-type").startsWith("image/") &&
                (it.attr("id").contains("cover", ignoreCase = true) ||
                    it.attr("href").contains("cover", ignoreCase = true))
        }

        val href = (epub3 ?: epub2 ?: byName)?.attr("href")?.takeIf { it.isNotBlank() } ?: return null
        return entries[normalizePath(if (basePath.isEmpty()) href else "$basePath/$href")]
    }

    /** Resolves "a/b/../c" style relative segments some OPFs produce. */
    private fun normalizePath(path: String): String {
        val stack = ArrayDeque<String>()
        for (segment in path.split('/')) {
            when (segment) {
                ".." -> stack.removeLastOrNull()
                ".", "" -> Unit
                else -> stack.addLast(segment)
            }
        }
        return stack.joinToString("/")
    }

    private fun readZipEntries(input: InputStream): Map<String, ByteArray> {
        val result = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    result[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return result
    }
}
