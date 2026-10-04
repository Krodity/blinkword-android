package uk.krodity.blinkword.data.discover

import uk.krodity.blinkword.data.importers.DocumentFormat

internal const val GUTENBERG = "https://www.gutenberg.org"

/** A Project Gutenberg title as listed on a search page. */
data class GutenbergBook(
    val id: Int,
    val title: String,
    val author: String?,
    val downloadCount: Int,
    val coverUrl: String? = null,
)

data class GutenbergDownload(val url: String, val format: DocumentFormat)

/** ePub first for its chapter structure; epub3 is the richest when it exists. */
private val PREFERRED_EPUBS = listOf(".epub3.images", ".epub.images", ".epub.noimages")

/**
 * Picks the file to download from the links on a book's page.
 *
 * Only the plain `/ebooks/<id>.<ext>` links are real downloads: `/ebooks/send/…`
 * are "send to Dropbox/Drive" helpers, and `/files/…` is where audiobooks keep
 * their readme — which is exactly the trap that would otherwise import a
 * readme as if it were the book. A recording has no readable edition at all,
 * so it correctly yields null here.
 */
fun pickDownload(hrefs: List<String>): GutenbergDownload? {
    val candidates = hrefs.filter { it.contains("/ebooks/") && !it.contains("/ebooks/send/") }

    for (suffix in PREFERRED_EPUBS) {
        candidates.firstOrNull { it.endsWith(suffix) }
            ?.let { return GutenbergDownload(it.toAbsoluteUrl(), DocumentFormat.EPUB) }
    }

    return candidates.firstOrNull { it.endsWith(".txt.utf-8") }
        ?.let { GutenbergDownload(it.toAbsoluteUrl(), DocumentFormat.PLAIN_TEXT) }
}

private fun String.toAbsoluteUrl(): String = if (startsWith("http")) this else "$GUTENBERG$this"

/**
 * Gutenberg prefixes some authors with a lowercase title -- "graf Leo Tolstoy".
 * Drop it; a leading all-lowercase word is never part of the name as written.
 */
fun formatAuthorName(raw: String): String {
    val trimmed = raw.trim()
    if (!trimmed.contains(' ')) return trimmed

    val firstWord = trimmed.substringBefore(' ')
    return if (firstWord == firstWord.lowercase()) trimmed.substringAfter(' ').trim() else trimmed
}
