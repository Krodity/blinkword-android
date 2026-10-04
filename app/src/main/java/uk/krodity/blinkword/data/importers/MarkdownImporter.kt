package uk.krodity.blinkword.data.importers

import uk.krodity.blinkword.logic.MarkdownPlainText

object MarkdownImporter {
    fun import(rawMarkdown: String, title: String): ImportedBook =
        ImportedBook(title, listOf(ImportedChapter(title, MarkdownPlainText.toPlainText(rawMarkdown))))
}
