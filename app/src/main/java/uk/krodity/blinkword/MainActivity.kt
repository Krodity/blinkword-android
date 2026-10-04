package uk.krodity.blinkword

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import uk.krodity.blinkword.ui.BlinkWordViewModel
import uk.krodity.blinkword.ui.Screen
import uk.krodity.blinkword.logic.achievements
import uk.krodity.blinkword.logic.computeReadingStats
import uk.krodity.blinkword.ui.screens.BlinkWordNavBar
import uk.krodity.blinkword.ui.screens.ChapterListSheet
import uk.krodity.blinkword.ui.screens.CollectionPickerSheet
import uk.krodity.blinkword.ui.screens.CreateCollectionDialog
import uk.krodity.blinkword.ui.screens.DiscoverScreen
import uk.krodity.blinkword.ui.screens.FullTextScreen
import uk.krodity.blinkword.ui.screens.ImportDialog
import uk.krodity.blinkword.ui.screens.LibraryScreen
import uk.krodity.blinkword.ui.screens.ReaderScreen
import uk.krodity.blinkword.ui.screens.SettingsSheet
import uk.krodity.blinkword.ui.screens.StatsScreen
import uk.krodity.blinkword.ui.theme.BlinkWordTheme
import uk.krodity.blinkword.ui.theme.readerColorsFor

class MainActivity : ComponentActivity() {
    private val vm: BlinkWordViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val ui by vm.ui.collectAsState()
            BlinkWordTheme(theme = ui.settings.theme) {
                AppRoot(vm)
            }
        }
    }
}

@Composable
private fun AppRoot(vm: BlinkWordViewModel) {
    val ui by vm.ui.collectAsState()

    BackHandler(enabled = ui.showFullText) { vm.hideFullText() }
    BackHandler(enabled = ui.screen == Screen.READER && !ui.showFullText) { vm.exitReader() }
    BackHandler(enabled = ui.screen == Screen.STATS || ui.screen == Screen.DISCOVER) { vm.showLibrary() }

    val readerColors = readerColorsFor(ui.settings.theme)
    val reader = ui.reader

    if (ui.showFullText && reader != null) {
        FullTextScreen(
            title = reader.title,
            tokens = reader.tokens,
            currentIndex = reader.currentIndex,
            wordColor = readerColors.word,
            highlightColor = readerColors.highlight,
            onBack = vm::hideFullText,
            onSelectWord = vm::jumpToWord,
        )
    } else {
        val navBar: @Composable () -> Unit = {
            BlinkWordNavBar(
                current = ui.screen,
                onLibrary = vm::showLibrary,
                onDiscover = vm::showDiscover,
                onStats = vm::showStats,
                onSettings = vm::showSettingsSheet,
            )
        }

        when (ui.screen) {
            Screen.DISCOVER -> DiscoverScreen(
                state = ui.discover,
                onQueryChange = vm::setDiscoverQuery,
                onSearch = vm::searchDiscover,
                onDownload = vm::downloadBook,
                bottomBar = navBar,
            )

            Screen.STATS -> {
                val stats = remember(ui.readingDays, ui.documents) {
                    computeReadingStats(ui.readingDays, ui.documents)
                }
                StatsScreen(
                    stats = stats,
                    achievements = remember(stats) { achievements(stats) },
                    dailyGoal = ui.settings.dailyWordGoal,
                    onSetDailyGoal = vm::setDailyWordGoal,
                    bottomBar = navBar,
                )
            }

            Screen.LIBRARY -> LibraryScreen(
                documents = ui.documents,
                collections = ui.collections,
                documentCollections = ui.documentCollections,
                selectedCollectionId = ui.selectedCollectionId,
                viewMode = ui.settings.libraryViewMode,
                thumbnailScale = ui.settings.libraryThumbnailScale,
                sortOrder = ui.settings.librarySortOrder,
                onOpen = vm::openDocument,
                onDelete = vm::deleteDocument,
                onSelectCollection = vm::selectCollection,
                onCreateCollection = vm::showCreateCollectionDialog,
                onDeleteCollection = vm::deleteCollection,
                onSetViewMode = vm::setLibraryViewMode,
                onSetSortOrder = vm::setLibrarySortOrder,
                onSetThumbnailScale = vm::setLibraryThumbnailScale,
                onAddToCollection = vm::showCollectionPicker,
                onShowImportDialog = vm::showImportDialog,
                bottomBar = navBar,
            )

            Screen.READER -> ui.reader?.let { reader ->
                ReaderScreen(
                    reader = reader,
                    fontSizeSp = ui.settings.fontSizeSp,
                    wordColor = readerColors.word,
                    highlightColor = readerColors.highlight,
                    showAnchorHighlight = ui.settings.showAnchorHighlight,
                    showFocusGuides = ui.settings.showFocusGuides,
                    onBack = vm::exitReader,
                    onShowSettings = vm::showSettingsSheet,
                    onShowChapters = vm::showChapterList,
                    onShowFullText = vm::showFullText,
                    onPlayPause = vm::playPause,
                    onNext = vm::next,
                    onPrevious = vm::previous,
                    onNextSentence = vm::nextSentence,
                    onPreviousSentence = vm::previousSentence,
                    onWpmChange = vm::setWpm,
                    onScrub = vm::scrubTo,
                    speechRate = ui.settings.speechRate,
                    onToggleSpeech = vm::toggleSpeech,
                    onSpeechRateChange = vm::setSpeechRate,
                )
            }
        }
    }

    if (ui.showImportDialog) {
        ImportDialog(
            onDismiss = vm::hideImportDialog,
            onImportPastedText = vm::importPastedText,
            onImportFromUri = vm::importFromUri,
        )
    }

    if (ui.showSettingsSheet) {
        SettingsSheet(
            theme = ui.settings.theme,
            fontSizeSp = ui.settings.fontSizeSp,
            showAnchorHighlight = ui.settings.showAnchorHighlight,
            showFocusGuides = ui.settings.showFocusGuides,
            onDismiss = vm::hideSettingsSheet,
            onThemeChange = vm::setTheme,
            onFontSizeChange = vm::setFontSize,
            onShowAnchorHighlightChange = vm::setShowAnchorHighlight,
            onShowFocusGuidesChange = vm::setShowFocusGuides,
            onExportBackup = vm::exportBackup,
            onImportBackup = vm::importBackup,
            voices = ui.availableVoices,
            selectedVoice = ui.settings.speechVoice,
            onVoiceChange = vm::setSpeechVoice,
            neuralState = ui.neural,
            useNeural = ui.settings.useNeuralTts,
            neuralModelId = ui.settings.neuralModelId,
            neuralSpeakerId = ui.settings.neuralSpeakerId,
            onToggleNeural = vm::setUseNeuralTts,
            onDownloadModel = vm::downloadNeuralModel,
            onDeleteModel = vm::deleteNeuralModel,
            onSelectNeuralVoice = { modelId, speakerId -> vm.setNeuralVoice(modelId, speakerId) },
            onPreviewNeuralVoice = { modelId, speakerId -> vm.previewNeuralVoice(modelId, speakerId) },
        )
    }

    if (ui.showChapterList) {
        ui.reader?.let { reader ->
            ChapterListSheet(
                chapters = reader.chapters,
                onDismiss = vm::hideChapterList,
                onSelect = vm::jumpToChapter,
            )
        }
    }

    if (ui.showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = vm::hideCreateCollectionDialog,
            onConfirm = vm::createCollection,
        )
    }

    ui.collectionPickerForDocument?.let { documentId ->
        val assignedIds = ui.documentCollections
            .filter { it.documentId == documentId }
            .map { it.collectionId }
            .toSet()
        CollectionPickerSheet(
            collections = ui.collections,
            assignedCollectionIds = assignedIds,
            onToggle = { collectionId, currentlyIn -> vm.toggleDocumentInCollection(documentId, collectionId, currentlyIn) },
            onCreateNew = vm::showCreateCollectionDialog,
            onDismiss = vm::hideCollectionPicker,
        )
    }
}
