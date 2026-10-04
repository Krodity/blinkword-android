package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.logic.RsvpToken

/** One paragraph's words flattened into a single space-joined string, plus each word's char range within it. */
private data class ParagraphLayout(
    val tokenStartIndex: Int,
    val text: String,
    val wordCharRanges: List<IntRange>,
)

private fun buildParagraphs(tokens: List<RsvpToken>): List<ParagraphLayout> {
    val paragraphs = mutableListOf<ParagraphLayout>()

    fun flush(start: Int, endInclusive: Int) {
        val sb = StringBuilder()
        val ranges = ArrayList<IntRange>(endInclusive - start + 1)
        for (i in start..endInclusive) {
            val wordStart = sb.length
            sb.append(tokens[i].text)
            ranges += wordStart until sb.length
            if (i != endInclusive) sb.append(' ')
        }
        paragraphs += ParagraphLayout(start, sb.toString(), ranges)
    }

    var start = 0
    tokens.forEachIndexed { index, token ->
        if (token.isParagraphEnd) {
            flush(start, index)
            start = index + 1
        }
    }
    if (start <= tokens.lastIndex) flush(start, tokens.lastIndex)
    return paragraphs
}

/** Finds the word range containing [charIndex], or null if it landed on inter-word whitespace. */
private fun List<IntRange>.wordAt(charIndex: Int): Int? {
    var low = 0
    var high = size - 1
    while (low <= high) {
        val mid = (low + high) / 2
        val range = this[mid]
        when {
            charIndex < range.first -> high = mid - 1
            charIndex > range.last -> low = mid + 1
            else -> return mid
        }
    }
    return null
}

/**
 * A scrollable view of the whole document's text, tap-to-resume from any
 * word. Built from [reader]'s already-tokenized word list (no extra DB read,
 * even for a book with hundreds of thousands of words) and laid out one
 * `Text` per paragraph rather than per word, so only visible paragraphs are
 * ever composed or measured.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FullTextScreen(
    title: String,
    tokens: List<RsvpToken>,
    currentIndex: Int,
    wordColor: Color,
    highlightColor: Color,
    onBack: () -> Unit,
    onSelectWord: (Int) -> Unit,
) {
    val paragraphs = remember(tokens) { buildParagraphs(tokens) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        val startParagraph = paragraphs.indexOfLast { it.tokenStartIndex <= currentIndex }.coerceAtLeast(0)
        listState.scrollToItem(startParagraph)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
        ) {
            itemsIndexed(paragraphs, key = { _, paragraph -> paragraph.tokenStartIndex }) { _, paragraph ->
                ParagraphText(
                    paragraph = paragraph,
                    currentIndex = currentIndex,
                    wordColor = wordColor,
                    highlightColor = highlightColor,
                    onSelectWord = onSelectWord,
                )
            }
        }
    }
}

@Composable
private fun ParagraphText(
    paragraph: ParagraphLayout,
    currentIndex: Int,
    wordColor: Color,
    highlightColor: Color,
    onSelectWord: (Int) -> Unit,
) {
    var layoutResult by remember { mutableStateOf<TextLayoutResult?>(null) }

    val annotated = remember(paragraph, currentIndex, highlightColor) {
        buildAnnotatedString {
            append(paragraph.text)
            val localCurrent = currentIndex - paragraph.tokenStartIndex
            paragraph.wordCharRanges.getOrNull(localCurrent)?.let { range ->
                addStyle(
                    SpanStyle(background = highlightColor.copy(alpha = 0.35f), fontWeight = FontWeight.Bold),
                    range.first,
                    range.last + 1,
                )
            }
        }
    }

    Text(
        text = annotated,
        color = wordColor,
        style = MaterialTheme.typography.bodyLarge,
        onTextLayout = { layoutResult = it },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .pointerInput(paragraph.tokenStartIndex) {
                detectTapGestures { tapOffset ->
                    val charIndex = layoutResult?.getOffsetForPosition(tapOffset) ?: return@detectTapGestures
                    paragraph.wordCharRanges.wordAt(charIndex)?.let { wordIndex ->
                        onSelectWord(paragraph.tokenStartIndex + wordIndex)
                    }
                }
            },
    )
}
