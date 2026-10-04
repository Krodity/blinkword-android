package uk.krodity.blinkword.data.importers

import android.content.Context
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import com.tom_roush.pdfbox.text.PDFTextStripper
import java.io.InputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Extracts text from a PDF. When the PDF has an outline (bookmarks), each
 * top-level bookmark becomes a chapter spanning to the next bookmark's page;
 * otherwise the whole document comes back as a single chapter.
 */
object PdfImporter {
    private val initialized = AtomicBoolean(false)

    fun import(context: Context, input: InputStream, fallbackTitle: String): ImportedBook {
        if (initialized.compareAndSet(false, true)) {
            PDFBoxResourceLoader.init(context.applicationContext)
        }

        PDDocument.load(input).use { document ->
            val title = document.documentInformation?.title?.takeIf { it.isNotBlank() } ?: fallbackTitle
            val bookmarks = document.documentCatalog?.documentOutline?.let { flattenOutline(it, document) }.orEmpty()

            if (bookmarks.size < 2) {
                val text = PDFTextStripper().getText(document)
                return ImportedBook(title, listOf(ImportedChapter(title, text)))
            }

            val chapters = bookmarks.mapIndexed { index, bookmark ->
                val stripper = PDFTextStripper()
                stripper.startPage = bookmark.pageIndex + 1
                stripper.endPage = if (index + 1 < bookmarks.size) bookmarks[index + 1].pageIndex else document.numberOfPages
                ImportedChapter(bookmark.title, stripper.getText(document))
            }
            return ImportedBook(title, chapters)
        }
    }

    private data class Bookmark(val title: String, val pageIndex: Int)

    private fun flattenOutline(outline: PDDocumentOutline, document: PDDocument): List<Bookmark> {
        val result = mutableListOf<Bookmark>()
        var item: PDOutlineItem? = outline.firstChild
        while (item != null) {
            val page = item.findDestinationPage(document)
            val pageIndex = page?.let { document.pages.indexOf(it) } ?: -1
            if (pageIndex >= 0) {
                result += Bookmark(item.title ?: "Chapter ${result.size + 1}", pageIndex)
            }
            item = item.nextSibling
        }
        return result.sortedBy { it.pageIndex }
    }
}
