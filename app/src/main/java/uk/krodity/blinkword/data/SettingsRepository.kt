package uk.krodity.blinkword.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("blinkword-settings")

enum class AppTheme { LIGHT, DARK, SEPIA }

enum class LibraryViewMode { LIST, GRID }
enum class ThumbnailScale { SMALL, MEDIUM, LARGE }
enum class LibrarySortOrder { TITLE, DATE_ADDED, LAST_OPENED, PROGRESS }

/** Persisted, app-wide reading defaults: last-used speed, theme, reader font size, and anchor display options. */
class SettingsRepository(private val context: Context) {

    data class Settings(
        val wpm: Int = 300,
        val theme: AppTheme = AppTheme.LIGHT,
        val fontSizeSp: Float = 48f,
        val showAnchorHighlight: Boolean = true,
        val showFocusGuides: Boolean = true,
        val libraryViewMode: LibraryViewMode = LibraryViewMode.LIST,
        val libraryThumbnailScale: ThumbnailScale = ThumbnailScale.MEDIUM,
        val librarySortOrder: LibrarySortOrder = LibrarySortOrder.DATE_ADDED,
        val dailyWordGoal: Int = 10_000,
        val speechRate: Float = 1.0f,
        /** Engine voice name; null means whatever the engine defaults to. */
        val speechVoice: String? = null,
        /** Use the downloaded neural engine instead of the system one. */
        val useNeuralTts: Boolean = false,
        val neuralModelId: String? = null,
        val neuralSpeakerId: Int = 0,
    )

    private companion object {
        val KEY_WPM = intPreferencesKey("wpm")
        val KEY_THEME = stringPreferencesKey("theme")
        val KEY_FONT_SIZE = floatPreferencesKey("font_size_sp")
        val KEY_SHOW_ANCHOR_HIGHLIGHT = booleanPreferencesKey("show_anchor_highlight")
        val KEY_SHOW_FOCUS_GUIDES = booleanPreferencesKey("show_focus_guides")
        val KEY_LIBRARY_VIEW_MODE = stringPreferencesKey("library_view_mode")
        val KEY_LIBRARY_THUMBNAIL_SCALE = stringPreferencesKey("library_thumbnail_scale")
        val KEY_LIBRARY_SORT_ORDER = stringPreferencesKey("library_sort_order")
        val KEY_DAILY_WORD_GOAL = intPreferencesKey("daily_word_goal")
        val KEY_SPEECH_RATE = floatPreferencesKey("speech_rate")
        val KEY_SPEECH_VOICE = stringPreferencesKey("speech_voice")
        val KEY_USE_NEURAL_TTS = booleanPreferencesKey("use_neural_tts")
        val KEY_NEURAL_MODEL_ID = stringPreferencesKey("neural_model_id")
        val KEY_NEURAL_SPEAKER_ID = intPreferencesKey("neural_speaker_id")
    }

    val settings: Flow<Settings> = context.dataStore.data.map { prefs ->
        Settings(
            wpm = (prefs[KEY_WPM] ?: 300).coerceIn(100, 1000),
            theme = prefs[KEY_THEME]?.let { runCatching { AppTheme.valueOf(it) }.getOrNull() } ?: AppTheme.LIGHT,
            fontSizeSp = prefs[KEY_FONT_SIZE] ?: 48f,
            showAnchorHighlight = prefs[KEY_SHOW_ANCHOR_HIGHLIGHT] ?: true,
            showFocusGuides = prefs[KEY_SHOW_FOCUS_GUIDES] ?: true,
            libraryViewMode = prefs[KEY_LIBRARY_VIEW_MODE]?.let { runCatching { LibraryViewMode.valueOf(it) }.getOrNull() }
                ?: LibraryViewMode.LIST,
            libraryThumbnailScale = prefs[KEY_LIBRARY_THUMBNAIL_SCALE]?.let { runCatching { ThumbnailScale.valueOf(it) }.getOrNull() }
                ?: ThumbnailScale.MEDIUM,
            librarySortOrder = prefs[KEY_LIBRARY_SORT_ORDER]?.let { runCatching { LibrarySortOrder.valueOf(it) }.getOrNull() }
                ?: LibrarySortOrder.DATE_ADDED,
            dailyWordGoal = (prefs[KEY_DAILY_WORD_GOAL] ?: 10_000).coerceIn(100, 1_000_000),
            speechRate = (prefs[KEY_SPEECH_RATE] ?: 1.0f).coerceIn(0.5f, 3f),
            speechVoice = prefs[KEY_SPEECH_VOICE]?.takeIf { it.isNotBlank() },
            useNeuralTts = prefs[KEY_USE_NEURAL_TTS] ?: false,
            neuralModelId = prefs[KEY_NEURAL_MODEL_ID]?.takeIf { it.isNotBlank() },
            neuralSpeakerId = prefs[KEY_NEURAL_SPEAKER_ID] ?: 0,
        )
    }

    suspend fun setWpm(wpm: Int) {
        context.dataStore.edit { it[KEY_WPM] = wpm.coerceIn(100, 1000) }
    }

    suspend fun setTheme(theme: AppTheme) {
        context.dataStore.edit { it[KEY_THEME] = theme.name }
    }

    suspend fun setFontSize(sp: Float) {
        context.dataStore.edit { it[KEY_FONT_SIZE] = sp }
    }

    suspend fun setShowAnchorHighlight(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_ANCHOR_HIGHLIGHT] = show }
    }

    suspend fun setShowFocusGuides(show: Boolean) {
        context.dataStore.edit { it[KEY_SHOW_FOCUS_GUIDES] = show }
    }

    suspend fun setLibraryViewMode(mode: LibraryViewMode) {
        context.dataStore.edit { it[KEY_LIBRARY_VIEW_MODE] = mode.name }
    }

    suspend fun setLibraryThumbnailScale(scale: ThumbnailScale) {
        context.dataStore.edit { it[KEY_LIBRARY_THUMBNAIL_SCALE] = scale.name }
    }

    suspend fun setLibrarySortOrder(order: LibrarySortOrder) {
        context.dataStore.edit { it[KEY_LIBRARY_SORT_ORDER] = order.name }
    }

    suspend fun setDailyWordGoal(words: Int) {
        context.dataStore.edit { it[KEY_DAILY_WORD_GOAL] = words.coerceIn(100, 1_000_000) }
    }

    suspend fun setSpeechRate(rate: Float) {
        context.dataStore.edit { it[KEY_SPEECH_RATE] = rate.coerceIn(0.5f, 3f) }
    }

    suspend fun setSpeechVoice(name: String?) {
        context.dataStore.edit {
            if (name.isNullOrBlank()) it.remove(KEY_SPEECH_VOICE) else it[KEY_SPEECH_VOICE] = name
        }
    }

    suspend fun setUseNeuralTts(enabled: Boolean) {
        context.dataStore.edit { it[KEY_USE_NEURAL_TTS] = enabled }
    }

    suspend fun setNeuralVoice(modelId: String?, speakerId: Int) {
        context.dataStore.edit {
            if (modelId == null) it.remove(KEY_NEURAL_MODEL_ID) else it[KEY_NEURAL_MODEL_ID] = modelId
            it[KEY_NEURAL_SPEAKER_ID] = speakerId.coerceAtLeast(0)
        }
    }

    /** Writes every setting at once, for restoring a backup. */
    suspend fun replaceAll(settings: Settings) {
        context.dataStore.edit {
            it[KEY_WPM] = settings.wpm.coerceIn(100, 1000)
            it[KEY_THEME] = settings.theme.name
            it[KEY_FONT_SIZE] = settings.fontSizeSp
            it[KEY_SHOW_ANCHOR_HIGHLIGHT] = settings.showAnchorHighlight
            it[KEY_SHOW_FOCUS_GUIDES] = settings.showFocusGuides
            it[KEY_LIBRARY_VIEW_MODE] = settings.libraryViewMode.name
            it[KEY_LIBRARY_THUMBNAIL_SCALE] = settings.libraryThumbnailScale.name
            it[KEY_LIBRARY_SORT_ORDER] = settings.librarySortOrder.name
            it[KEY_DAILY_WORD_GOAL] = settings.dailyWordGoal.coerceIn(100, 1_000_000)
            it[KEY_SPEECH_RATE] = settings.speechRate.coerceIn(0.5f, 3f)
            settings.speechVoice?.let { voice -> it[KEY_SPEECH_VOICE] = voice }
            it[KEY_USE_NEURAL_TTS] = settings.useNeuralTts
            settings.neuralModelId?.let { id -> it[KEY_NEURAL_MODEL_ID] = id }
            it[KEY_NEURAL_SPEAKER_ID] = settings.neuralSpeakerId.coerceAtLeast(0)
        }
    }
}
