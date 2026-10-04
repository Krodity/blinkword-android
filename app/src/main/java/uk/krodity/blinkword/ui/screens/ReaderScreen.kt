package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RecordVoiceOver
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.ui.ReaderState

/** WPM slider snaps to multiples of [WPM_STEP] across its 100..1000 range. */
private const val WPM_MIN = 100
private const val WPM_MAX = 1000
private const val WPM_STEP = 5
private const val WPM_SLIDER_STEPS = (WPM_MAX - WPM_MIN) / WPM_STEP - 1

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ReaderScreen(
    reader: ReaderState,
    fontSizeSp: Float,
    wordColor: Color,
    highlightColor: Color,
    showAnchorHighlight: Boolean,
    showFocusGuides: Boolean,
    onBack: () -> Unit,
    onShowSettings: () -> Unit,
    onShowChapters: () -> Unit,
    onShowFullText: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onNextSentence: () -> Unit,
    onPreviousSentence: () -> Unit,
    onWpmChange: (Int) -> Unit,
    onScrub: (Float) -> Unit,
    speechRate: Float,
    onToggleSpeech: () -> Unit,
    onSpeechRateChange: (Float) -> Unit,
) {
    var showWpmDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(reader.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (reader.chapters.size > 1) {
                        IconButton(onClick = onShowChapters) {
                            Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Chapters")
                        }
                    }
                    IconButton(onClick = onShowSettings) {
                        Icon(Icons.Filled.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .combinedClickable(onClick = onPlayPause, onLongClick = onShowFullText),
                contentAlignment = Alignment.Center,
            ) {
                if (reader.isSpeaking) {
                    SpokenSentence(reader = reader, wordColor = wordColor, highlightColor = highlightColor)
                } else {
                    RsvpWordDisplay(
                        token = reader.tokens.getOrNull(reader.currentIndex),
                        fontSizeSp = fontSizeSp,
                        wordColor = wordColor,
                        highlightColor = highlightColor,
                        showAnchorHighlight = showAnchorHighlight,
                        showFocusGuides = showFocusGuides,
                    )
                }
            }

            Text(
                text = "Long-press the word to browse the full text",
                style = MaterialTheme.typography.bodySmall,
                color = wordColor.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )

            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Slider(
                    value = reader.currentIndex.toFloat().coerceIn(0f, reader.tokens.lastIndex.coerceAtLeast(0).toFloat()),
                    onValueChange = { onScrub(it / reader.tokens.lastIndex.coerceAtLeast(1)) },
                    valueRange = 0f..reader.tokens.lastIndex.coerceAtLeast(0).toFloat(),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    IconButton(onClick = onPreviousSentence) {
                        Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous sentence")
                    }
                    IconButton(onClick = onPrevious) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous word")
                    }
                    IconButton(onClick = onPlayPause) {
                        Icon(
                            if (reader.isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (reader.isPlaying) "Pause" else "Play",
                        )
                    }
                    IconButton(onClick = onNext) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next word")
                    }
                    IconButton(onClick = onNextSentence) {
                        Icon(Icons.Filled.SkipNext, contentDescription = "Next sentence")
                    }
                    IconButton(onClick = onToggleSpeech) {
                        Icon(
                            if (reader.isSpeaking) Icons.Filled.VolumeOff else Icons.Filled.VolumeUp,
                            contentDescription = if (reader.isSpeaking) "Stop reading aloud" else "Read aloud",
                            tint = if (reader.isSpeaking) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                LocalContentColor.current
                            },
                        )
                    }
                }

                if (reader.isSpeaking) {
                    Text("Voice speed ${"%.1f".format(speechRate)}×")
                    Slider(
                        value = speechRate,
                        onValueChange = onSpeechRateChange,
                        valueRange = 0.5f..3f,
                        steps = 24, // 0.1x increments
                    )
                } else {
                    Text(
                        text = "${reader.wpm} WPM",
                        modifier = Modifier.clickable { showWpmDialog = true },
                    )
                    val wpmSliderColors = SliderDefaults.colors()
                    Slider(
                        value = reader.wpm.toFloat(),
                        onValueChange = { onWpmChange(it.toInt()) },
                        valueRange = WPM_MIN.toFloat()..WPM_MAX.toFloat(),
                        steps = WPM_SLIDER_STEPS,
                        // With 179 steps the default tick dots sit closer together than their
                        // own diameter and merge into a solid line through the track. Coloring
                        // them the same as their track segment hides them while keeping real
                        // snap-to-step dragging from `steps`.
                        colors = wpmSliderColors.copy(
                            activeTickColor = wpmSliderColors.activeTrackColor,
                            inactiveTickColor = wpmSliderColors.inactiveTrackColor,
                        ),
                    )
                }
            }
        }
    }

    if (showWpmDialog) {
        WpmInputDialog(
            initialWpm = reader.wpm,
            onDismiss = { showWpmDialog = false },
            onConfirm = {
                onWpmChange(it)
                showWpmDialog = false
            },
        )
    }
}

/**
 * What the reader shows while the voice is reading: the sentence being spoken,
 * with a little of what came before and after for context, so the screen stays
 * oriented in the book rather than flashing single words out of time with the
 * audio.
 */
@Composable
private fun SpokenSentence(reader: ReaderState, wordColor: Color, highlightColor: Color) {
    val sentence = reader.currentSentence ?: return

    val text = remember(reader.tokens, sentence) {
        buildAnnotatedString {
            val leadIn = (sentence.first - 12).coerceAtLeast(0)
            if (leadIn < sentence.first) {
                withStyle(SpanStyle(color = wordColor.copy(alpha = 0.35f))) {
                    append(reader.tokens.subList(leadIn, sentence.first).joinToString(" ") { it.text })
                    append(" ")
                }
            }

            withStyle(SpanStyle(color = wordColor, fontWeight = FontWeight.Medium)) {
                append(reader.tokens.slice(sentence).joinToString(" ") { it.text })
            }

            val tailEnd = (sentence.last + 13).coerceAtMost(reader.tokens.size)
            if (sentence.last + 1 < tailEnd) {
                withStyle(SpanStyle(color = wordColor.copy(alpha = 0.35f))) {
                    append(" ")
                    append(reader.tokens.subList(sentence.last + 1, tailEnd).joinToString(" ") { it.text })
                }
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            Icons.Filled.RecordVoiceOver,
            contentDescription = null,
            tint = highlightColor,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        Text(
            text = text,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun WpmInputDialog(initialWpm: Int, onDismiss: () -> Unit, onConfirm: (Int) -> Unit) {
    var text by remember { mutableStateOf(initialWpm.toString()) }
    val parsed = text.toIntOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set WPM") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it.filter(Char::isDigit) },
                label = { Text("Words per minute ($WPM_MIN–$WPM_MAX)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
        },
        confirmButton = {
            TextButton(onClick = { parsed?.let(onConfirm) }, enabled = parsed != null) {
                Text("Set")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
