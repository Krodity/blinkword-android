package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.data.Collection

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionPickerSheet(
    collections: List<Collection>,
    assignedCollectionIds: Set<Long>,
    onToggle: (collectionId: Long, currentlyIn: Boolean) -> Unit,
    onCreateNew: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Text(
            text = "Add to collection",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
        )

        if (collections.isEmpty()) {
            Text(
                text = "No collections yet.",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
        } else {
            LazyColumn {
                items(collections, key = { it.id }) { collection ->
                    val checked = collection.id in assignedCollectionIds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(collection.id, checked) }
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { onToggle(collection.id, checked) })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(collection.name)
                    }
                }
            }
        }

        TextButton(onClick = onCreateNew, modifier = Modifier.padding(16.dp)) {
            Text("+ New collection")
        }
    }
}
