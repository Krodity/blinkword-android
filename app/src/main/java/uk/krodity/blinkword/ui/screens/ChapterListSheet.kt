package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.data.Chapter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChapterListSheet(
    chapters: List<Chapter>,
    onDismiss: () -> Unit,
    onSelect: (Chapter) -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            items(chapters, key = { it.id }) { chapter ->
                Text(
                    text = chapter.title,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(chapter) }
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                )
            }
        }
    }
}
