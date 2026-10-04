package uk.krodity.blinkword.ui

import android.app.Application
import android.net.Uri
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import uk.krodity.blinkword.data.AppTheme
import uk.krodity.blinkword.data.Chapter
import uk.krodity.blinkword.data.Collection
import uk.krodity.blinkword.data.DocumentCollectionCrossRef
import uk.krodity.blinkword.data.DocumentRepository
import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.NetworkProblem
import uk.krodity.blinkword.data.message
import uk.krodity.blinkword.data.networkProblem
import uk.krodity.blinkword.data.discover.GutenbergApi
import uk.krodity.blinkword.data.discover.GutenbergBook
import uk.krodity.blinkword.data.LibrarySortOrder
import uk.krodity.blinkword.data.LibraryViewMode
import uk.krodity.blinkword.data.ReadingDay
import uk.krodity.blinkword.data.SettingsRepository
import uk.krodity.blinkword.data.StatsRepository
import uk.krodity.blinkword.data.ThumbnailScale
import uk.krodity.blinkword.data.backup.BackupRepository
import uk.krodity.blinkword.data.tts.NeuralModel
import uk.krodity.blinkword.data.tts.PREVIEW_SENTENCE
import uk.krodity.blinkword.data.tts.NeuralModelRepository
import uk.krodity.blinkword.data.tts.NeuralTtsEngine
import uk.krodity.blinkword.data.tts.TtsController
import uk.krodity.blinkword.data.tts.TtsVoice
import uk.krodity.blinkword.logic.RsvpToken
import uk.krodity.blinkword.logic.RsvpTokenizer
import uk.krodity.blinkword.logic.WordTiming
import uk.krodity.blinkword.logic.sentenceIndexFor
import uk.krodity.blinkword.logic.sentenceRanges
import kotlin.math.roundToInt

enum class Screen { LIBRARY, READER, STATS, DISCOVER }

data class NeuralTtsState(
    val installedIds: Set<String> = emptySet(),
    val downloadingId: String? = null,
    val progress: Float = 0f,
    val previewingId: String? = null,
    /** How many voices a model turned out to hold; only known once it's loaded. */
    val speakerCounts: Map<String, Int> = emptyMap(),
)

data class DiscoverState(
    val query: String = "",
    val results: List<GutenbergBook> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    /** Set when Android itself is keeping this app off the network. */
    val networkProblem: NetworkProblem? = null,
    val downloadingIds: Set<Int> = emptySet(),
    val downloadedIds: Set<Int> = emptySet(),
)

data class ReaderState(
    val documentId: Long,
    val title: String,
    val tokens: List<RsvpToken>,
    val currentIndex: Int,
    val isPlaying: Boolean,
    val wpm: Int,
    val chapters: List<Chapter> = emptyList(),
    /** Precomputed once per document -- deriving these per sentence is O(book). */
    val sentences: List<IntRange> = emptyList(),
    val isSpeaking: Boolean = false,
) {
    /** The chunk text-to-speech is currently on, as a range over [tokens]. */
    val currentSentence: IntRange?
        get() = sentences.getOrNull(sentenceIndexFor(sentences, currentIndex))
}

data class UiState(
    val screen: Screen = Screen.LIBRARY,
    val documents: List<DocumentSummary> = emptyList(),
    val collections: List<Collection> = emptyList(),
    val documentCollections: List<DocumentCollectionCrossRef> = emptyList(),
    val selectedCollectionId: Long? = null,
    val readingDays: List<ReadingDay> = emptyList(),
    val discover: DiscoverState = DiscoverState(),
    val availableVoices: List<TtsVoice> = emptyList(),
    val neural: NeuralTtsState = NeuralTtsState(),
    val settings: SettingsRepository.Settings = SettingsRepository.Settings(),
    val reader: ReaderState? = null,
    val showImportDialog: Boolean = false,
    val showSettingsSheet: Boolean = false,
    val showChapterList: Boolean = false,
    val showFullText: Boolean = false,
    val showCreateCollectionDialog: Boolean = false,
    val collectionPickerForDocument: Long? = null,
)

class BlinkWordViewModel(application: Application) : AndroidViewModel(application) {
    private val documents = DocumentRepository(application)
    private val settingsRepo = SettingsRepository(application)
    private val stats = StatsRepository(application)
    private val backups = BackupRepository(application, documents, settingsRepo)
    private val tts = TtsController(application)
    private val neuralModels = NeuralModelRepository(application)
    private val neuralEngine = NeuralTtsEngine()
    private var neuralJob: Job? = null

    private val _ui = MutableStateFlow(UiState())
    val ui: StateFlow<UiState> = _ui

    private var playbackJob: Job? = null
    private var saveSettingsJob: Job? = null

    /** Words advanced and wall-clock elapsed since the last stats flush. */
    private var sessionWords = 0
    private var sessionStartMillis = 0L

    init {
        viewModelScope.launch {
            documents.observeSummaries().collect { list ->
                _ui.update { it.copy(documents = list) }
            }
        }
        viewModelScope.launch {
            documents.observeCollections().collect { list ->
                _ui.update { it.copy(collections = list) }
            }
        }
        viewModelScope.launch {
            documents.observeCollectionMemberships().collect { list ->
                _ui.update { it.copy(documentCollections = list) }
            }
        }
        viewModelScope.launch {
            stats.observeDays().collect { days ->
                _ui.update { it.copy(readingDays = days) }
            }
        }
        refreshInstalledModels()
        viewModelScope.launch {
            settingsRepo.settings.collect { settings ->
                _ui.update { it.copy(settings = settings) }
            }
        }
    }

    // --- Library ---

    fun showImportDialog() = _ui.update { it.copy(showImportDialog = true) }
    fun hideImportDialog() = _ui.update { it.copy(showImportDialog = false) }
    fun showSettingsSheet() {
        _ui.update { it.copy(showSettingsSheet = true) }
        loadVoices()
    }

    /** The engine only lists its voices once it has started, so ask on demand. */
    private fun loadVoices() {
        if (_ui.value.availableVoices.isNotEmpty()) return
        tts.ensureReady { available ->
            if (!available) return@ensureReady
            tts.setRate(_ui.value.settings.speechRate)
            tts.setVoice(_ui.value.settings.speechVoice)
            _ui.update { it.copy(availableVoices = tts.availableVoices()) }
        }
    }
    fun hideSettingsSheet() = _ui.update { it.copy(showSettingsSheet = false) }
    fun showChapterList() = _ui.update { it.copy(showChapterList = true) }
    fun hideChapterList() = _ui.update { it.copy(showChapterList = false) }

    fun showLibrary() = _ui.update { it.copy(screen = Screen.LIBRARY) }
    fun showStats() = _ui.update { it.copy(screen = Screen.STATS) }

    fun showDiscover() {
        _ui.update { it.copy(screen = Screen.DISCOVER) }
        // First visit lands on Gutendex's most-downloaded list.
        if (_ui.value.discover.results.isEmpty() && !_ui.value.discover.isLoading) searchDiscover()
    }

    fun setDiscoverQuery(query: String) =
        _ui.update { it.copy(discover = it.discover.copy(query = query)) }

    fun searchDiscover() {
        val query = _ui.value.discover.query
        _ui.update { it.copy(discover = it.discover.copy(isLoading = true, error = null, networkProblem = null)) }

        viewModelScope.launch {
            try {
                val results = GutenbergApi.search(query)
                _ui.update { it.copy(discover = it.discover.copy(results = results, isLoading = false)) }
            } catch (e: Exception) {
                _ui.update {
                    it.copy(
                        discover = it.discover.copy(
                            isLoading = false,
                            error = e.message ?: "Couldn't reach Project Gutenberg",
                            networkProblem = networkProblem(getApplication()),
                        ),
                    )
                }
            }
        }
    }

    fun downloadBook(book: GutenbergBook) = viewModelScope.launch {
        _ui.update { it.copy(discover = it.discover.copy(downloadingIds = it.discover.downloadingIds + book.id)) }
        try {
            val download = GutenbergApi.resolveDownload(book.id)
            documents.importFromUrl(
                url = download.url,
                fallbackTitle = book.title,
                fallbackAuthor = book.author,
                format = download.format,
                coverUrl = book.coverUrl,
            )
            _ui.update { it.copy(discover = it.discover.copy(downloadedIds = it.discover.downloadedIds + book.id)) }
        } catch (e: Exception) {
            val reason = networkProblem(getApplication())?.message() ?: e.message
            Toast.makeText(getApplication(), "Download failed: $reason", Toast.LENGTH_LONG).show()
        } finally {
            _ui.update { it.copy(discover = it.discover.copy(downloadingIds = it.discover.downloadingIds - book.id)) }
        }
    }

    fun openAppSettings() = uk.krodity.blinkword.data.openAppSettings(getApplication())

    fun selectCollection(id: Long?) = _ui.update { it.copy(selectedCollectionId = id) }

    fun setLibraryViewMode(mode: LibraryViewMode) = viewModelScope.launch { settingsRepo.setLibraryViewMode(mode) }
    fun setLibrarySortOrder(order: LibrarySortOrder) = viewModelScope.launch { settingsRepo.setLibrarySortOrder(order) }
    fun setLibraryThumbnailScale(scale: ThumbnailScale) =
        viewModelScope.launch { settingsRepo.setLibraryThumbnailScale(scale) }

    fun showCreateCollectionDialog() = _ui.update { it.copy(showCreateCollectionDialog = true) }
    fun hideCreateCollectionDialog() = _ui.update { it.copy(showCreateCollectionDialog = false) }

    fun createCollection(name: String) = viewModelScope.launch {
        if (name.isBlank()) return@launch
        documents.createCollection(name)
        hideCreateCollectionDialog()
    }

    fun deleteCollection(id: Long) = viewModelScope.launch {
        documents.deleteCollection(id)
        if (_ui.value.selectedCollectionId == id) selectCollection(null)
    }

    fun showCollectionPicker(documentId: Long) = _ui.update { it.copy(collectionPickerForDocument = documentId) }
    fun hideCollectionPicker() = _ui.update { it.copy(collectionPickerForDocument = null) }

    fun toggleDocumentInCollection(documentId: Long, collectionId: Long, currentlyIn: Boolean) = viewModelScope.launch {
        if (currentlyIn) documents.removeFromCollection(documentId, collectionId)
        else documents.addToCollection(documentId, collectionId)
    }

    fun showFullText() {
        stopSpeaking()
        pauseInternal()
        _ui.update { it.copy(showFullText = true) }
    }

    fun hideFullText() = _ui.update { it.copy(showFullText = false) }

    /** Resumes RSVP playback (paused) from a word tapped in the full-text browser. */
    fun jumpToWord(index: Int) {
        hideFullText()
        jumpTo(index)
    }

    fun importPastedText(title: String, body: String) = viewModelScope.launch {
        if (body.isBlank()) return@launch
        runImport { documents.importText(title, body) }
    }

    fun importFromUri(uri: Uri) = viewModelScope.launch {
        runImport { documents.importFromUri(uri) }
    }

    private suspend fun runImport(block: suspend () -> Long) {
        try {
            block()
            hideImportDialog()
        } catch (e: Exception) {
            Toast.makeText(getApplication(), "Import failed: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun deleteDocument(id: Long) = viewModelScope.launch {
        documents.delete(id)
    }

    fun openDocument(id: Long) = viewModelScope.launch {
        val doc = documents.getById(id) ?: return@launch
        val tokens = RsvpTokenizer.tokenize(documents.getContent(doc.id))
        val startIndex = doc.lastReadWordIndex.coerceIn(0, (tokens.size - 1).coerceAtLeast(0))
        val chapters = documents.getChapters(doc.id)
        val sentences = sentenceRanges(tokens)
        _ui.update {
            it.copy(
                screen = Screen.READER,
                reader = ReaderState(
                    documentId = doc.id,
                    title = doc.title,
                    tokens = tokens,
                    currentIndex = startIndex,
                    isPlaying = false,
                    wpm = it.settings.wpm,
                    chapters = chapters,
                    sentences = sentences,
                ),
            )
        }
    }

    fun exitReader() {
        stopSpeaking()
        pauseInternal()
        _ui.update { it.copy(screen = Screen.LIBRARY, reader = null) }
    }

    // --- Text to speech ---

    fun toggleSpeech() {
        if (_ui.value.reader?.isSpeaking == true) stopSpeaking() else startSpeaking()
    }

    private fun startSpeaking() {
        pauseInternal() // RSVP and speech are two ways to read the same position.

        val settings = _ui.value.settings
        val modelId = settings.neuralModelId
        if (settings.useNeuralTts && modelId != null && modelId in _ui.value.neural.installedIds) {
            startNeuralSpeaking(modelId, settings.neuralSpeakerId)
            return
        }

        tts.onUtteranceDone = { onSentenceSpoken() }
        tts.ensureReady { available ->
            if (!available) {
                toast("Text-to-speech isn't available on this device")
                return@ensureReady
            }
            tts.setRate(_ui.value.settings.speechRate)
            tts.setVoice(_ui.value.settings.speechVoice)
            sessionStartMillis = System.currentTimeMillis()
            _ui.update { it.copy(reader = it.reader?.copy(isSpeaking = true)) }
            speakCurrentSentence()
        }
    }

    /**
     * The neural engine synthesises a sentence at a time and blocks while it
     * plays, so it drives its own loop off the main thread rather than being
     * called back by the platform engine.
     */
    private fun startNeuralSpeaking(modelId: String, speakerId: Int) {
        neuralJob?.cancel()
        sessionStartMillis = System.currentTimeMillis()
        _ui.update { it.copy(reader = it.reader?.copy(isSpeaking = true)) }

        neuralJob = viewModelScope.launch(Dispatchers.Default) {
            val layout = neuralModels.layoutFor(modelId)
            if (layout == null) {
                withContext(Dispatchers.Main) {
                    toast("That voice isn't installed any more")
                    stopSpeaking()
                }
                return@launch
            }

            runCatching { neuralEngine.load(modelId, layout) }.onFailure {
                withContext(Dispatchers.Main) {
                    toast("Couldn't load the voice: ${it.message}")
                    stopSpeaking()
                }
                return@launch
            }

            val speaker = speakerId.coerceIn(0, (neuralEngine.speakerCount - 1).coerceAtLeast(0))

            while (isActive) {
                val reader = _ui.value.reader ?: break
                if (!reader.isSpeaking) break
                val sentence = reader.currentSentence ?: break

                val text = reader.tokens.slice(sentence).joinToString(" ") { it.text }
                val finished = runCatching {
                    neuralEngine.speakBlocking(text, speaker, _ui.value.settings.speechRate)
                }.getOrDefault(false)

                if (!finished || !isActive) break

                sessionWords += sentence.count()
                val next = reader.sentences.getOrNull(sentenceIndexFor(reader.sentences, reader.currentIndex) + 1)
                if (next == null) {
                    withContext(Dispatchers.Main) { stopSpeaking() }
                    break
                }

                _ui.update { it.copy(reader = it.reader?.copy(currentIndex = next.first)) }
                persistProgress(reader.documentId, next.first)
                flushReadingStats()
            }
        }
    }

    private fun stopSpeaking() {
        if (_ui.value.reader?.isSpeaking != true) return
        neuralJob?.cancel()
        neuralJob = null
        neuralEngine.stop()
        tts.stop()
        _ui.update { it.copy(reader = it.reader?.copy(isSpeaking = false)) }
        flushReadingStats()
        _ui.value.reader?.let { persistProgress(it.documentId, it.currentIndex) }
    }

    private fun speakCurrentSentence() {
        val reader = _ui.value.reader ?: return
        val sentence = reader.currentSentence ?: return stopSpeaking()
        val text = reader.tokens.slice(sentence).joinToString(" ") { it.text }
        tts.speak(text, sentence.first.toString())
    }

    /** Engine callback: bank what was read, then move to the next chunk. */
    private fun onSentenceSpoken() = viewModelScope.launch {
        val reader = _ui.value.reader ?: return@launch
        if (!reader.isSpeaking) return@launch

        val spokenIndex = sentenceIndexFor(reader.sentences, reader.currentIndex)
        sessionWords += reader.sentences.getOrNull(spokenIndex)?.count() ?: 0

        val next = reader.sentences.getOrNull(spokenIndex + 1)
        if (next == null) {
            stopSpeaking()
            return@launch
        }

        _ui.update { it.copy(reader = it.reader?.copy(currentIndex = next.first)) }
        persistProgress(reader.documentId, next.first)
        flushReadingStats()
        speakCurrentSentence()
    }

    fun setSpeechRate(rate: Float) {
        val clamped = rate.coerceIn(0.5f, 3f)
        tts.setRate(clamped)
        viewModelScope.launch { settingsRepo.setSpeechRate(clamped) }

        // The engine applies a new rate to the next utterance, so restart the
        // current sentence to make the change audible straight away.
        if (_ui.value.reader?.isSpeaking == true) speakCurrentSentence()
    }

    // --- Neural voices ---

    private fun refreshInstalledModels() = viewModelScope.launch(Dispatchers.IO) {
        val installed = neuralModels.installedIds()
        _ui.update { it.copy(neural = it.neural.copy(installedIds = installed)) }
    }

    fun downloadNeuralModel(model: NeuralModel) = viewModelScope.launch {
        if (_ui.value.neural.downloadingId != null) return@launch
        _ui.update { it.copy(neural = it.neural.copy(downloadingId = model.id, progress = 0f)) }

        try {
            neuralModels.install(model) { fraction ->
                _ui.update { it.copy(neural = it.neural.copy(progress = fraction)) }
            }
            val installed = withContext(Dispatchers.IO) { neuralModels.installedIds() }
            _ui.update {
                it.copy(
                    neural = it.neural.copy(installedIds = installed, downloadingId = null, progress = 0f),
                )
            }
            // First voice downloaded becomes the active one.
            if (_ui.value.settings.neuralModelId == null) setNeuralVoice(model.id, 0)
            toast("${model.displayName} ready")
        } catch (e: Exception) {
            _ui.update { it.copy(neural = it.neural.copy(downloadingId = null, progress = 0f)) }
            toast("Download failed: ${networkProblem(getApplication())?.message() ?: e.message}")
        }
    }

    fun deleteNeuralModel(modelId: String) = viewModelScope.launch {
        if (_ui.value.settings.neuralModelId == modelId) {
            neuralEngine.release()
            settingsRepo.setNeuralVoice(null, 0)
        }
        withContext(Dispatchers.IO) { neuralModels.delete(modelId) }
        refreshInstalledModels()
    }

    fun setUseNeuralTts(enabled: Boolean) = viewModelScope.launch {
        settingsRepo.setUseNeuralTts(enabled)
        if (_ui.value.reader?.isSpeaking == true) {
            stopSpeaking()
            startSpeaking()
        }
    }

    fun setNeuralVoice(modelId: String?, speakerId: Int) = viewModelScope.launch {
        settingsRepo.setNeuralVoice(modelId, speakerId)
        if (modelId != null) loadSpeakerCount(modelId)
        if (_ui.value.reader?.isSpeaking == true) {
            stopSpeaking()
            startSpeaking()
        }
    }

    /** Speaks a sample so voices can be compared without starting a book. */
    fun previewNeuralVoice(modelId: String, speakerId: Int) {
        stopSpeaking()
        neuralJob?.cancel()
        neuralEngine.stop()

        neuralJob = viewModelScope.launch(Dispatchers.Default) {
            _ui.update { it.copy(neural = it.neural.copy(previewingId = modelId)) }
            try {
                val layout = neuralModels.layoutFor(modelId) ?: return@launch
                neuralEngine.load(modelId, layout)
                recordSpeakerCount(modelId, neuralEngine.speakerCount)

                val speaker = speakerId.coerceIn(0, (neuralEngine.speakerCount - 1).coerceAtLeast(0))
                neuralEngine.speakBlocking(PREVIEW_SENTENCE, speaker, _ui.value.settings.speechRate)
            } catch (e: Throwable) {
                withContext(Dispatchers.Main) { toast("Preview failed: ${e.message}") }
            } finally {
                _ui.update { it.copy(neural = it.neural.copy(previewingId = null)) }
            }
        }
    }

    /** Loading is the only way to learn a model's voice count, so do it off the main thread. */
    private fun loadSpeakerCount(modelId: String) = viewModelScope.launch(Dispatchers.Default) {
        if (_ui.value.neural.speakerCounts.containsKey(modelId)) return@launch
        val layout = neuralModels.layoutFor(modelId) ?: return@launch
        runCatching {
            neuralEngine.load(modelId, layout)
            recordSpeakerCount(modelId, neuralEngine.speakerCount)
        }
    }

    private fun recordSpeakerCount(modelId: String, count: Int) {
        if (count <= 0) return
        _ui.update {
            it.copy(neural = it.neural.copy(speakerCounts = it.neural.speakerCounts + (modelId to count)))
        }
    }

    fun setSpeechVoice(name: String?) {
        tts.setVoice(name)
        viewModelScope.launch { settingsRepo.setSpeechVoice(name) }
        if (_ui.value.reader?.isSpeaking == true) speakCurrentSentence()
    }

    // --- Playback ---

    fun playPause() {
        if (_ui.value.reader?.isPlaying == true) pauseInternal() else startPlayback()
    }

    private fun startPlayback() {
        playbackJob?.cancel()
        sessionWords = 0
        sessionStartMillis = System.currentTimeMillis()
        _ui.update { it.copy(reader = it.reader?.copy(isPlaying = true)) }
        playbackJob = viewModelScope.launch {
            while (isActive) {
                val reader = _ui.value.reader ?: break
                val token = reader.tokens.getOrNull(reader.currentIndex) ?: break
                delay(WordTiming.wordDelayMs(reader.wpm, token.toTimingInput()))

                val next = reader.currentIndex + 1
                if (next > reader.tokens.lastIndex) {
                    pauseInternal()
                    break
                }
                _ui.update { it.copy(reader = it.reader?.copy(currentIndex = next)) }
                sessionWords++
                if (next % 20 == 0) {
                    persistProgress(reader.documentId, next)
                    flushReadingStats()
                }
            }
        }
    }

    private fun pauseInternal() {
        playbackJob?.cancel()
        playbackJob = null
        flushReadingStats()
        val reader = _ui.value.reader ?: return
        _ui.update { it.copy(reader = reader.copy(isPlaying = false)) }
        persistProgress(reader.documentId, reader.currentIndex)
    }

    /**
     * Banks the words and time accumulated since the last flush. Only time spent
     * with playback actually running counts, so a paused reader left open doesn't
     * inflate the day's reading time.
     */
    private fun flushReadingStats() {
        val words = sessionWords
        val startedAt = sessionStartMillis
        val now = System.currentTimeMillis()
        val millis = if (startedAt == 0L) 0L else now - startedAt
        val wpm = _ui.value.reader?.wpm ?: _ui.value.settings.wpm

        sessionWords = 0
        val stillReading = playbackJob != null || _ui.value.reader?.isSpeaking == true
        sessionStartMillis = if (stillReading) now else 0L

        if (words <= 0 && millis <= 0L) return
        viewModelScope.launch { stats.record(words = words, millis = millis, wpm = wpm) }
    }

    fun setWpm(wpm: Int) {
        val clamped = wpm.coerceIn(100, 1000)
        _ui.update { it.copy(reader = it.reader?.copy(wpm = clamped)) }
        // The playback loop rereads reader.wpm fresh every iteration, so this
        // takes effect starting the next word -- no restart needed here.
        debouncedSaveDefaultWpm(clamped)
    }

    private fun jumpTo(index: Int) {
        val reader = _ui.value.reader ?: return
        if (reader.tokens.isEmpty()) return
        val wasPlaying = reader.isPlaying
        playbackJob?.cancel()
        playbackJob = null
        flushReadingStats()

        val clamped = index.coerceIn(0, reader.tokens.lastIndex)
        _ui.update { it.copy(reader = reader.copy(currentIndex = clamped, isPlaying = false)) }
        persistProgress(reader.documentId, clamped)

        // Jumping while listening should pick the voice up at the new position.
        if (reader.isSpeaking) speakCurrentSentence()
        if (wasPlaying) startPlayback()
    }

    fun next() = jumpTo((_ui.value.reader?.currentIndex ?: 0) + 1)
    fun previous() = jumpTo((_ui.value.reader?.currentIndex ?: 0) - 1)

    fun nextSentence() {
        val reader = _ui.value.reader ?: return
        val from = reader.currentIndex
        val target = reader.tokens.withIndex()
            .firstOrNull { (i, token) -> i > from && token.endsSentence }
            ?.index ?: reader.tokens.lastIndex
        jumpTo(target)
    }

    fun previousSentence() {
        val reader = _ui.value.reader ?: return
        val from = reader.currentIndex
        // Walk back past the sentence end immediately behind us, to the one before that.
        val target = reader.tokens.withIndex()
            .filter { (i, token) -> i < from - 1 && token.endsSentence }
            .lastOrNull()
            ?.index?.plus(1) ?: 0
        jumpTo(target)
    }

    fun scrubTo(fraction: Float) {
        val reader = _ui.value.reader ?: return
        jumpTo((fraction * reader.tokens.lastIndex).roundToInt())
    }

    fun jumpToChapter(chapter: Chapter) {
        hideChapterList()
        jumpTo(chapter.startWordIndex)
    }

    private fun persistProgress(documentId: Long, index: Int) = viewModelScope.launch {
        documents.updateProgress(documentId, index)
    }

    private fun debouncedSaveDefaultWpm(wpm: Int) {
        saveSettingsJob?.cancel()
        saveSettingsJob = viewModelScope.launch {
            delay(500)
            settingsRepo.setWpm(wpm)
        }
    }

    // --- Settings ---

    fun setTheme(theme: AppTheme) = viewModelScope.launch { settingsRepo.setTheme(theme) }
    fun setFontSize(sp: Float) = viewModelScope.launch { settingsRepo.setFontSize(sp) }
    fun setShowAnchorHighlight(show: Boolean) = viewModelScope.launch { settingsRepo.setShowAnchorHighlight(show) }
    fun setShowFocusGuides(show: Boolean) = viewModelScope.launch { settingsRepo.setShowFocusGuides(show) }
    fun setDailyWordGoal(words: Int) = viewModelScope.launch { settingsRepo.setDailyWordGoal(words) }

    // --- Backup ---

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        try {
            backups.exportTo(uri)
            toast("Library exported")
        } catch (e: Exception) {
            toast("Export failed: ${e.message}")
        }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        toast("Restoring backup…")
        try {
            val summary = backups.importFrom(uri)
            toast(
                buildString {
                    append("Restored ${summary.total} book(s): ")
                    append("${summary.downloaded} re-downloaded, ")
                    append("${summary.progressRestored} progress restored")
                    if (summary.unavailable > 0) append(", ${summary.unavailable} need re-importing")
                }
            )
        } catch (e: Exception) {
            toast("Restore failed: ${e.message}")
        }
    }

    private fun toast(message: String) =
        Toast.makeText(getApplication(), message, Toast.LENGTH_LONG).show()

    override fun onCleared() {
        pauseInternal()
        tts.shutdown()
        neuralEngine.release()
        super.onCleared()
    }
}
