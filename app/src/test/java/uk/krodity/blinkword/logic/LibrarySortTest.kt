package uk.krodity.blinkword.logic

import org.junit.Assert.assertEquals
import org.junit.Test
import uk.krodity.blinkword.data.DocumentCollectionCrossRef
import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.LibrarySortOrder

private fun doc(
    id: Long,
    title: String,
    createdAt: Long,
    updatedAt: Long = createdAt,
    wordCount: Int = 100,
    lastReadWordIndex: Int = 0,
) = DocumentSummary(id, title, author = null, format = "EPUB", coverPath = null, wordCount, lastReadWordIndex, createdAt, updatedAt)

class LibrarySortTest {

    private val library = listOf(
        doc(1, "Charlotte's Web", createdAt = 10, updatedAt = 30, lastReadWordIndex = 90),
        doc(2, "Beowulf", createdAt = 30, updatedAt = 10, lastReadWordIndex = 10),
        doc(3, "alice in wonderland", createdAt = 20, updatedAt = 20, lastReadWordIndex = 50),
    )

    @Test
    fun `sorts by title case-insensitively`() {
        val result = libraryItems(library, emptyList(), collectionId = null, sortOrder = LibrarySortOrder.TITLE)
        assertEquals(listOf(3L, 2L, 1L), result.map { it.id })
    }

    @Test
    fun `sorts by date added, newest first`() {
        val result = libraryItems(library, emptyList(), collectionId = null, sortOrder = LibrarySortOrder.DATE_ADDED)
        assertEquals(listOf(2L, 3L, 1L), result.map { it.id })
    }

    @Test
    fun `sorts by last opened, most recent first`() {
        val result = libraryItems(library, emptyList(), collectionId = null, sortOrder = LibrarySortOrder.LAST_OPENED)
        assertEquals(listOf(1L, 3L, 2L), result.map { it.id })
    }

    @Test
    fun `sorts by progress, most complete first`() {
        val result = libraryItems(library, emptyList(), collectionId = null, sortOrder = LibrarySortOrder.PROGRESS)
        assertEquals(listOf(1L, 3L, 2L), result.map { it.id })
    }

    @Test
    fun `filters to only documents in the selected collection`() {
        val crossRefs = listOf(
            DocumentCollectionCrossRef(documentId = 1, collectionId = 100),
            DocumentCollectionCrossRef(documentId = 3, collectionId = 100),
            DocumentCollectionCrossRef(documentId = 2, collectionId = 200),
        )

        val result = libraryItems(library, crossRefs, collectionId = 100, sortOrder = LibrarySortOrder.TITLE)

        assertEquals(listOf(3L, 1L), result.map { it.id })
    }

    @Test
    fun `null collectionId returns every document`() {
        val result = libraryItems(library, emptyList(), collectionId = null, sortOrder = LibrarySortOrder.TITLE)
        assertEquals(3, result.size)
    }
}
