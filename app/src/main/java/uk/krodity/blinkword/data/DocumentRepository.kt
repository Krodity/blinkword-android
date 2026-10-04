package uk.krodity.blinkword.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.InputStream
import uk.krodity.blinkword.data.discover.httpGet
import uk.krodity.blinkword.data.importers.DocumentFormat
import uk.krodity.blinkword.data.importers.EpubImporter
import uk.krodity.blinkword.data.importers.FormatDetector
import uk.krodity.blinkword.data.importers.ImportedBook
import uk.krodity.blinkword.data.importers.ImportedChapter
import uk.krodity.blinkword.data.importers.MarkdownImporter
import uk.krodity.blinkword.data.importers.PdfImporter
import uk.krodity.blinkword.data.importers.PlainTextImporter
import uk.krodity.blinkword.logic.RsvpTokenizer

private const val CHAPTER_SEPARATOR = "\n\n"

/**
 * Max characters per [ContentChunk] row. Comfortably under SQLite's ~2MB
 * CursorWindow-per-row limit even at UTF-8's worst case of 4 bytes/char
 * (200_000 * 4 = ~780KB), independent of how the source document happens to
 * be chaptered.
 */
private const val CONTENT_CHUNK_SIZE = 200_000

/** Owns document persistence: Room access plus turning pasted text / a picked file into a [Document]. */
class DocumentRepository(private val context: Context) {
    private val database = AppDatabase.get(context)
    private val documentDao = database.documentDao()
    private val chapterDao = database.chapterDao()
    private val contentChunkDao = database.contentChunkDao()
    private val collectionDao = database.collectionDao()

    fun observeSummaries(): Flow<List<DocumentSummary>> = documentDao.observeSummaries()

    fun observeCollections(): Flow<List<Collection>> = collectionDao.observeAll()

    fun observeCollectionMemberships(): Flow<List<DocumentCollectionCrossRef>> = collectionDao.observeCrossRefs()

    suspend fun createCollection(name: String): Long =
        collectionDao.insert(Collection(name = name.trim(), createdAt = System.currentTimeMillis()))

    suspend fun deleteCollection(id: Long) = collectionDao.delete(id)

    suspend fun addToCollection(documentId: Long, collectionId: Long) =
        collectionDao.assign(DocumentCollectionCrossRef(documentId, collectionId))

    suspend fun removeFromCollection(documentId: Long, collectionId: Long) =
        collectionDao.unassign(documentId, collectionId)

    suspend fun getById(id: Long): Document? = documentDao.getById(id)

    suspend fun getContent(id: Long): String =
        contentChunkDao.getForDocument(id).joinToString("") { it.text }

    suspend fun getChapters(documentId: Long): List<Chapter> = chapterDao.getForDocument(documentId)

    suspend fun delete(id: Long) = documentDao.delete(id)

    suspend fun updateProgress(id: Long, index: Int) =
        documentDao.updateProgress(id, index, System.currentTimeMillis())

    suspend fun importText(title: String, content: String): Long = withContext(Dispatchers.IO) {
        saveBook(PlainTextImporter.import(content, title.trim().ifEmpty { "Untitled" }), format = "TEXT")
    }

    /**
     * Downloads a book and stores it. The source's own metadata wins when it has
     * any -- an ePub's `dc:creator` is more precise than a catalogue listing --
     * with [fallbackAuthor] filling in for formats that carry no metadata at all.
     */
    suspend fun importFromUrl(
        url: String,
        fallbackTitle: String,
        fallbackAuthor: String?,
        format: DocumentFormat,
        coverUrl: String? = null,
    ): Long = withContext(Dispatchers.IO) {
        val book = httpGet(url) { stream ->
            when (format) {
                DocumentFormat.EPUB -> EpubImporter.import(stream, fallbackTitle)
                DocumentFormat.PDF -> PdfImporter.import(context, stream, fallbackTitle)
                DocumentFormat.MARKDOWN -> MarkdownImporter.import(stream.reader().readText(), fallbackTitle)
                DocumentFormat.PLAIN_TEXT -> PlainTextImporter.import(stream.reader().readText(), fallbackTitle)
            }
        }

        // A plain-text edition carries no cover, so fall back to the catalogue's.
        val cover = book.coverImage ?: coverUrl?.let {
            runCatching { httpGet(it) { stream -> stream.readBytes() } }.getOrNull()
        }

        saveBook(
            book.copy(author = book.author ?: fallbackAuthor, coverImage = cover),
            format = format.name,
            sourceUrl = url,
        )
    }

    /** Reads a `content://` Uri and stores it as a new document, dispatching by detected format. */
    suspend fun importFromUri(uri: Uri): Long = withContext(Dispatchers.IO) {
        val displayName = queryDisplayName(uri) ?: "Imported document"
        val fallbackTitle = displayName.substringBeforeLast('.', displayName)
        val format = FormatDetector.detect(displayName)

        val book = when (format) {
            DocumentFormat.EPUB -> openStream(uri) { EpubImporter.import(it, fallbackTitle) }
            DocumentFormat.PDF -> openStream(uri) { PdfImporter.import(context, it, fallbackTitle) }
            DocumentFormat.MARKDOWN -> MarkdownImporter.import(readText(uri), fallbackTitle)
            DocumentFormat.PLAIN_TEXT -> PlainTextImporter.import(readText(uri), fallbackTitle)
        }
        saveBook(book, format = format.name)
    }

    private suspend fun saveBook(book: ImportedBook, format: String, sourceUrl: String? = null): Long {
        val chapters = book.chapters.ifEmpty { listOf(ImportedChapter(book.title, "")) }
        val content = chapters.joinToString(CHAPTER_SEPARATOR) { it.text }
        val wordCount = RsvpTokenizer.tokenize(content).size
        val now = System.currentTimeMillis()

        val documentId = documentDao.insert(
            Document(
                title = book.title,
                author = book.author,
                format = format,
                sourceUrl = sourceUrl,
                wordCount = wordCount,
                createdAt = now,
                updatedAt = now,
            )
        )

        val contentChunks = content.chunked(CONTENT_CHUNK_SIZE).mapIndexed { index, text ->
            ContentChunk(documentId = documentId, chunkIndex = index, text = text)
        }
        contentChunkDao.insertAll(contentChunks)

        book.coverImage?.let { bytes ->
            runCatching { saveCover(documentId, bytes) }
                .getOrNull()
                ?.let { documentDao.updateCoverPath(documentId, it) }
        }

        if (chapters.size > 1) {
            var wordOffset = 0
            val chapterRows = chapters.mapIndexed { index, chapter ->
                val row = Chapter(
                    documentId = documentId,
                    orderIndex = index,
                    title = chapter.title,
                    startWordIndex = wordOffset,
                )
                wordOffset += RsvpTokenizer.tokenize(chapter.text).size
                row
            }
            chapterDao.insertAll(chapterRows)
        }

        return documentId
    }

    private fun saveCover(documentId: Long, bytes: ByteArray): String {
        val directory = java.io.File(context.filesDir, "covers").apply { mkdirs() }
        val file = java.io.File(directory, "$documentId.img")
        file.writeBytes(bytes)
        return file.absolutePath
    }

    private fun readText(uri: Uri): String =
        context.contentResolver.openInputStream(uri)?.use { it.bufferedReader().readText() } ?: ""

    private fun <T> openStream(uri: Uri, block: (InputStream) -> T): T =
        context.contentResolver.openInputStream(uri)?.use(block)
            ?: throw IllegalArgumentException("Could not open $uri")

    private fun queryDisplayName(uri: Uri): String? =
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0 && cursor.moveToFirst()) cursor.getString(nameIndex) else null
        }
}
