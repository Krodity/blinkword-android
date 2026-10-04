package uk.krodity.blinkword.data.importers

object PlainTextImporter {
    fun import(rawText: String, title: String): ImportedBook =
        ImportedBook(title, listOf(ImportedChapter(title, rawText)))
}
