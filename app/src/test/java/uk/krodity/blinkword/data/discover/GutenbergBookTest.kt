package uk.krodity.blinkword.data.discover

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import uk.krodity.blinkword.data.importers.DocumentFormat

/** The download links Project Gutenberg lists on a text book's page (id 2600). */
private val TEXT_BOOK_LINKS = listOf(
    "/ebooks/2600.epub.images",
    "/ebooks/2600.epub.noimages",
    "/ebooks/2600.epub3.images",
    "/ebooks/2600.kf8.images",
    "/ebooks/2600.txt.utf-8",
    "https://www.gutenberg.org/ebooks/send/dropbox/2600.epub3.images",
    "https://www.gutenberg.org/ebooks/send/gdrive/2600.epub.images",
)

/** An audiobook's page (id 28920) offers only a readme. */
private val AUDIOBOOK_LINKS = listOf("/files/28920/28920_readme.txt")

class GutenbergBookTest {

    @Test
    fun `prefers epub3 over the other editions`() {
        assertEquals(
            GutenbergDownload("https://www.gutenberg.org/ebooks/2600.epub3.images", DocumentFormat.EPUB),
            pickDownload(TEXT_BOOK_LINKS),
        )
    }

    @Test
    fun `falls back through epub editions then plain text`() {
        val noEpub3 = TEXT_BOOK_LINKS - "/ebooks/2600.epub3.images"
        assertEquals(
            GutenbergDownload("https://www.gutenberg.org/ebooks/2600.epub.images", DocumentFormat.EPUB),
            pickDownload(noEpub3),
        )

        assertEquals(
            GutenbergDownload("https://www.gutenberg.org/ebooks/2600.txt.utf-8", DocumentFormat.PLAIN_TEXT),
            pickDownload(listOf("/ebooks/2600.txt.utf-8", "/ebooks/2600.rdf")),
        )
    }

    @Test
    fun `never picks a send-to-cloud helper link`() {
        val onlyHelpers = listOf(
            "https://www.gutenberg.org/ebooks/send/dropbox/2600.epub3.images",
            "https://www.gutenberg.org/ebooks/send/gdrive/2600.epub.images",
        )

        assertNull(pickDownload(onlyHelpers))
    }

    @Test
    fun `an audiobook readme is not treated as the book`() {
        assertNull(pickDownload(AUDIOBOOK_LINKS))
    }

    @Test
    fun `drops a lowercase honorific from the author`() {
        assertEquals("Leo Tolstoy", formatAuthorName("graf Leo Tolstoy"))
    }

    @Test
    fun `leaves ordinary author names alone`() {
        assertEquals("H. G. Wells", formatAuthorName("H. G. Wells"))
        assertEquals("Aristophanes", formatAuthorName("Aristophanes"))
        assertEquals("John Maynard Keynes", formatAuthorName("John Maynard Keynes"))
    }

    @Test
    fun `parses a search results page`() {
        val html = """
            <ul>
              <li class="booklink">
                <a class="link" href="/ebooks/2600">
                  <img class="cover-thumb" src="/cache/epub/2600/pg2600.cover.small.jpg">
                  <span class="cell content">
                    <span class="title">War and Peace</span>
                    <span class="subtitle">graf Leo Tolstoy</span>
                    <span class="extra">38955 downloads</span>
                  </span>
                </a>
              </li>
              <li class="booklink">
                <a class="link" href="/ebooks/7700">
                  <span class="title">Lysistrata</span>
                  <span class="subtitle">Aristophanes</span>
                  <span class="extra">17106 downloads</span>
                </a>
              </li>
            </ul>
        """.trimIndent()

        val books = GutenbergApi.parseSearchResults(html)

        assertEquals(2, books.size)
        assertEquals(
            GutenbergBook(
                id = 2600,
                title = "War and Peace",
                author = "Leo Tolstoy",
                downloadCount = 38955,
                coverUrl = "https://www.gutenberg.org/cache/epub/2600/pg2600.cover.small.jpg",
            ),
            books[0],
        )
        // No thumbnail in the markup, so no cover to show.
        assertEquals(GutenbergBook(7700, "Lysistrata", "Aristophanes", 17106, null), books[1])
    }

    @Test
    fun `skips entries with no title`() {
        val html = """<li class="booklink"><a class="link" href="/ebooks/1"></a></li>"""

        assertEquals(emptyList<GutenbergBook>(), GutenbergApi.parseSearchResults(html))
    }
}
