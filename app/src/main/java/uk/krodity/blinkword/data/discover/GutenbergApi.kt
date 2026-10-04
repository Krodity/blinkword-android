package uk.krodity.blinkword.data.discover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup
import java.io.IOException
import java.net.URLEncoder

/**
 * Reads Project Gutenberg's own site. It publishes no JSON API, so the search
 * pages are parsed with jsoup -- the same parser the ePub importer already
 * uses. Download links are resolved from a book's page rather than guessed
 * from its id, since not every title offers every format.
 */
object GutenbergApi {

    suspend fun search(query: String): List<GutenbergBook> = withContext(Dispatchers.IO) {
        val url = if (query.isBlank()) {
            "$GUTENBERG/ebooks/search/?sort_order=downloads"
        } else {
            "$GUTENBERG/ebooks/search/?query=" + URLEncoder.encode(query.trim(), "UTF-8")
        }

        val html = httpGet(url) { it.reader().readText() }
        parseSearchResults(html)
    }

    /** Finds the best readable file for a book, or fails if it has none (a recording). */
    suspend fun resolveDownload(bookId: Int): GutenbergDownload = withContext(Dispatchers.IO) {
        val html = httpGet("$GUTENBERG/ebooks/$bookId") { it.reader().readText() }
        val hrefs = Jsoup.parse(html, GUTENBERG).select("a[href]").map { it.attr("href") }

        pickDownload(hrefs) ?: throw IOException("No readable text edition (this is likely an audiobook)")
    }

    internal fun parseSearchResults(html: String): List<GutenbergBook> =
        Jsoup.parse(html, GUTENBERG).select("li.booklink").mapNotNull { element ->
            val href = element.selectFirst("a.link")?.attr("href").orEmpty()
            val id = href.substringAfterLast("/ebooks/").toIntOrNull() ?: return@mapNotNull null

            val title = element.selectFirst("span.title")?.text()?.trim().orEmpty()
            if (title.isEmpty()) return@mapNotNull null

            val author = element.selectFirst("span.subtitle")?.text()
                ?.takeIf { it.isNotBlank() }
                ?.let { formatAuthorName(it) }

            GutenbergBook(
                id = id,
                title = title,
                author = author,
                downloadCount = element.selectFirst("span.extra")?.text()
                    ?.filter(Char::isDigit)
                    ?.toIntOrNull() ?: 0,
                // The listing shows a small thumbnail; the medium one is the same
                // image at a size worth keeping once the book is downloaded.
                coverUrl = element.selectFirst("img.cover-thumb")?.absUrl("src")?.takeIf { it.isNotBlank() },
            )
        }
}
