package uk.krodity.blinkword.data.importers

/** One chapter's plain text, as recovered from a source file. */
data class ImportedChapter(val title: String, val text: String)

/** The result of parsing any supported source format into RSVP-ready text. */
data class ImportedBook(
    val title: String,
    val chapters: List<ImportedChapter>,
    val author: String? = null,
    /** Raw cover image bytes, when the source carried one. */
    val coverImage: ByteArray? = null,
)

enum class DocumentFormat { PLAIN_TEXT, MARKDOWN, EPUB, PDF }

object FormatDetector {
    /** Detects format from the file's display name; anything unrecognized is treated as plain text. */
    fun detect(displayName: String): DocumentFormat = when (displayName.substringAfterLast('.', "").lowercase()) {
        "epub" -> DocumentFormat.EPUB
        "pdf" -> DocumentFormat.PDF
        "md", "markdown" -> DocumentFormat.MARKDOWN
        else -> DocumentFormat.PLAIN_TEXT
    }
}
