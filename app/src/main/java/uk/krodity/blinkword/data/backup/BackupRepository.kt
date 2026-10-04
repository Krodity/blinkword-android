package uk.krodity.blinkword.data.backup

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import uk.krodity.blinkword.data.AppDatabase
import uk.krodity.blinkword.data.AppTheme
import uk.krodity.blinkword.data.Collection
import uk.krodity.blinkword.data.DocumentCollectionCrossRef
import uk.krodity.blinkword.data.DocumentRepository
import uk.krodity.blinkword.data.LibrarySortOrder
import uk.krodity.blinkword.data.LibraryViewMode
import uk.krodity.blinkword.data.ReadingDay
import uk.krodity.blinkword.data.SettingsRepository
import uk.krodity.blinkword.data.ThumbnailScale
import uk.krodity.blinkword.data.importers.DocumentFormat
import uk.krodity.blinkword.logic.mergeProgress
import uk.krodity.blinkword.logic.mergeReadingDays
import java.io.IOException

private const val BACKUP_VERSION = 1

/** What a restore actually managed to do, for reporting back to the user. */
data class BackupSummary(
    val progressRestored: Int,
    val downloaded: Int,
    val unavailable: Int,
) {
    val total: Int get() = progressRestored + downloaded + unavailable
}

/**
 * Exports and restores the library as JSON.
 *
 * Book *text* is deliberately left out -- a backup stays a few kilobytes
 * instead of hundreds of megabytes. Anything downloaded from Discover carries
 * its source URL and is re-fetched on restore; books imported from local files
 * can't be re-fetched, so their reading position is held until the same title
 * is imported again.
 */
class BackupRepository(
    private val context: Context,
    private val documents: DocumentRepository,
    private val settingsRepo: SettingsRepository,
) {
    private val database = AppDatabase.get(context)
    private val documentDao = database.documentDao()
    private val collectionDao = database.collectionDao()
    private val readingDayDao = database.readingDayDao()

    suspend fun exportTo(uri: Uri): Unit = withContext(Dispatchers.IO) {
        val json = buildBackup()
        context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray()) }
            ?: throw IOException("Couldn't open $uri for writing")
    }

    suspend fun importFrom(uri: Uri): BackupSummary = withContext(Dispatchers.IO) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.reader().readText() }
            ?: throw IOException("Couldn't open $uri for reading")
        restore(JSONObject(text))
    }

    private suspend fun buildBackup(): String {
        val settings = settingsRepo.settings.first()
        val allDocuments = documentDao.getAll()
        val collections = collectionDao.getAll().associateBy { it.id }
        val crossRefs = collectionDao.getAllCrossRefs().groupBy { it.documentId }

        val documentsJson = JSONArray()
        for (document in allDocuments) {
            val names = crossRefs[document.id].orEmpty().mapNotNull { collections[it.collectionId]?.name }
            documentsJson.put(
                JSONObject().apply {
                    put("title", document.title)
                    put("author", document.author ?: JSONObject.NULL)
                    put("format", document.format)
                    put("sourceUrl", document.sourceUrl ?: JSONObject.NULL)
                    put("wordCount", document.wordCount)
                    put("lastReadWordIndex", document.lastReadWordIndex)
                    put("createdAt", document.createdAt)
                    put("updatedAt", document.updatedAt)
                    put("collections", JSONArray(names))
                }
            )
        }

        val daysJson = JSONArray()
        for (day in readingDayDao.getAll()) {
            daysJson.put(
                JSONObject().apply {
                    put("date", day.date)
                    put("wordsRead", day.wordsRead)
                    put("millisRead", day.millisRead)
                    put("maxWpm", day.maxWpm)
                }
            )
        }

        return JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("collections", JSONArray(collections.values.map { it.name }))
            put("documents", documentsJson)
            put("readingDays", daysJson)
            put(
                "settings",
                JSONObject().apply {
                    put("wpm", settings.wpm)
                    put("theme", settings.theme.name)
                    put("fontSizeSp", settings.fontSizeSp.toDouble())
                    put("showAnchorHighlight", settings.showAnchorHighlight)
                    put("showFocusGuides", settings.showFocusGuides)
                    put("libraryViewMode", settings.libraryViewMode.name)
                    put("libraryThumbnailScale", settings.libraryThumbnailScale.name)
                    put("librarySortOrder", settings.librarySortOrder.name)
                    put("dailyWordGoal", settings.dailyWordGoal)
                    put("speechRate", settings.speechRate.toDouble())
                    put("speechVoice", settings.speechVoice ?: JSONObject.NULL)
                },
            )
        }.toString(2)
    }

    private suspend fun restore(backup: JSONObject): BackupSummary {
        val version = backup.optInt("version", 1)
        if (version > BACKUP_VERSION) {
            throw IOException("This backup was made by a newer version of BlinkWord")
        }

        backup.optJSONObject("settings")?.let { restoreSettings(it) }
        restoreReadingDays(backup.optJSONArray("readingDays"))

        val collectionIds = restoreCollections(backup.optJSONArray("collections"))

        var progressRestored = 0
        var downloaded = 0
        var unavailable = 0

        val documentsJson = backup.optJSONArray("documents") ?: JSONArray()
        for (index in 0 until documentsJson.length()) {
            val entry = documentsJson.getJSONObject(index)
            val title = entry.optString("title").ifBlank { continue }
            val savedIndex = entry.optInt("lastReadWordIndex", 0)
            val sourceUrl = entry.optStringOrNull("sourceUrl")

            val existing = documentDao.findByTitle(title)
            val documentId = when {
                existing != null -> {
                    documentDao.updateProgress(
                        existing.id,
                        mergeProgress(existing.lastReadWordIndex, savedIndex),
                        entry.optLong("updatedAt", System.currentTimeMillis()),
                    )
                    progressRestored++
                    existing.id
                }

                sourceUrl != null -> {
                    val format = runCatching { DocumentFormat.valueOf(entry.optString("format")) }
                        .getOrDefault(DocumentFormat.EPUB)
                    val newId = runCatching {
                        documents.importFromUrl(
                            url = sourceUrl,
                            fallbackTitle = title,
                            fallbackAuthor = entry.optStringOrNull("author"),
                            format = format,
                        )
                    }.getOrNull()

                    if (newId == null) {
                        unavailable++
                        continue
                    }
                    documentDao.updateProgress(newId, savedIndex, entry.optLong("updatedAt", System.currentTimeMillis()))
                    downloaded++
                    newId
                }

                else -> {
                    // Imported from a local file that this backup can't reproduce.
                    unavailable++
                    continue
                }
            }

            val names = entry.optJSONArray("collections") ?: JSONArray()
            for (nameIndex in 0 until names.length()) {
                collectionIds[names.getString(nameIndex)]?.let { collectionId ->
                    collectionDao.assign(DocumentCollectionCrossRef(documentId, collectionId))
                }
            }
        }

        return BackupSummary(progressRestored, downloaded, unavailable)
    }

    private suspend fun restoreCollections(names: JSONArray?): Map<String, Long> {
        val existing = collectionDao.getAll().associate { it.name to it.id }.toMutableMap()
        if (names == null) return existing

        for (index in 0 until names.length()) {
            val name = names.getString(index)
            if (name !in existing) {
                existing[name] = collectionDao.insert(Collection(name = name, createdAt = System.currentTimeMillis()))
            }
        }
        return existing
    }

    private suspend fun restoreReadingDays(days: JSONArray?) {
        if (days == null) return

        val incoming = (0 until days.length()).map { index ->
            val day = days.getJSONObject(index)
            ReadingDay(
                date = day.getString("date"),
                wordsRead = day.optInt("wordsRead", 0),
                millisRead = day.optLong("millisRead", 0L),
                maxWpm = day.optInt("maxWpm", 0),
            )
        }

        mergeReadingDays(readingDayDao.getAll(), incoming).forEach { readingDayDao.upsert(it) }
    }

    private suspend fun restoreSettings(settings: JSONObject) {
        settingsRepo.replaceAll(
            SettingsRepository.Settings(
                wpm = settings.optInt("wpm", 300),
                theme = settings.enumOrDefault("theme", AppTheme.LIGHT),
                fontSizeSp = settings.optDouble("fontSizeSp", 48.0).toFloat(),
                showAnchorHighlight = settings.optBoolean("showAnchorHighlight", true),
                showFocusGuides = settings.optBoolean("showFocusGuides", true),
                libraryViewMode = settings.enumOrDefault("libraryViewMode", LibraryViewMode.LIST),
                libraryThumbnailScale = settings.enumOrDefault("libraryThumbnailScale", ThumbnailScale.MEDIUM),
                librarySortOrder = settings.enumOrDefault("librarySortOrder", LibrarySortOrder.DATE_ADDED),
                dailyWordGoal = settings.optInt("dailyWordGoal", 10_000),
                speechRate = settings.optDouble("speechRate", 1.0).toFloat(),
                speechVoice = settings.optStringOrNull("speechVoice"),
            )
        )
    }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotBlank() }

private inline fun <reified T : Enum<T>> JSONObject.enumOrDefault(key: String, default: T): T =
    runCatching { enumValueOf<T>(optString(key)) }.getOrDefault(default)
