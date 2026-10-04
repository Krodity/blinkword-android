package uk.krodity.blinkword.logic

import uk.krodity.blinkword.data.DocumentCollectionCrossRef
import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.LibrarySortOrder

/**
 * Narrows [documents] to one collection (or leaves them all when [collectionId]
 * is null) and applies [sortOrder]. A library is small enough (a personal
 * reading list, not a catalog) that doing this in Kotlin rather than SQL is
 * simpler and just as fast, and keeps it unit-testable without a database.
 */
fun libraryItems(
    documents: List<DocumentSummary>,
    crossRefs: List<DocumentCollectionCrossRef>,
    collectionId: Long?,
    sortOrder: LibrarySortOrder,
): List<DocumentSummary> {
    val filtered = if (collectionId == null) {
        documents
    } else {
        val idsInCollection = crossRefs.asSequence()
            .filter { it.collectionId == collectionId }
            .map { it.documentId }
            .toSet()
        documents.filter { it.id in idsInCollection }
    }

    return when (sortOrder) {
        LibrarySortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
        LibrarySortOrder.DATE_ADDED -> filtered.sortedByDescending { it.createdAt }
        LibrarySortOrder.LAST_OPENED -> filtered.sortedByDescending { it.updatedAt }
        LibrarySortOrder.PROGRESS -> filtered.sortedByDescending { it.progress }
    }
}
