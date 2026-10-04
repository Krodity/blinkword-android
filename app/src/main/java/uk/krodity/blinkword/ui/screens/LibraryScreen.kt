package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.data.Collection
import uk.krodity.blinkword.data.DocumentCollectionCrossRef
import uk.krodity.blinkword.data.DocumentSummary
import uk.krodity.blinkword.data.LibrarySortOrder
import uk.krodity.blinkword.data.LibraryViewMode
import uk.krodity.blinkword.data.ThumbnailScale
import uk.krodity.blinkword.logic.libraryItems
import uk.krodity.blinkword.logic.relativeTime

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    documents: List<DocumentSummary>,
    collections: List<Collection>,
    documentCollections: List<DocumentCollectionCrossRef>,
    selectedCollectionId: Long?,
    viewMode: LibraryViewMode,
    thumbnailScale: ThumbnailScale,
    sortOrder: LibrarySortOrder,
    onOpen: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSelectCollection: (Long?) -> Unit,
    onCreateCollection: () -> Unit,
    onDeleteCollection: (Long) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onSetSortOrder: (LibrarySortOrder) -> Unit,
    onSetThumbnailScale: (ThumbnailScale) -> Unit,
    onAddToCollection: (Long) -> Unit,
    onShowImportDialog: () -> Unit,
    bottomBar: @Composable () -> Unit,
) {
    var searchQuery by remember { mutableStateOf("") }

    val items = remember(documents, documentCollections, selectedCollectionId, sortOrder, searchQuery) {
        libraryItems(documents, documentCollections, selectedCollectionId, sortOrder)
            .filter { searchQuery.isBlank() || it.title.contains(searchQuery, ignoreCase = true) }
    }

    Scaffold(
        topBar = {
            LibraryHeader(
                viewMode = viewMode,
                sortOrder = sortOrder,
                searchQuery = searchQuery,
                onSearchQueryChange = { searchQuery = it },
                onSetViewMode = onSetViewMode,
                onSetSortOrder = onSetSortOrder,
                onShowImportDialog = onShowImportDialog,
                collections = collections,
                selectedCollectionId = selectedCollectionId,
                onSelectCollection = onSelectCollection,
                onCreateCollection = onCreateCollection,
                onDeleteCollection = onDeleteCollection,
            )
        },
        bottomBar = bottomBar,
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (viewMode == LibraryViewMode.GRID) {
                ScaleRow(current = thumbnailScale, onSet = onSetThumbnailScale)
            }

            when {
                items.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = when {
                                documents.isEmpty() -> "No documents yet. Tap + to paste text or import a .txt file."
                                searchQuery.isNotBlank() -> "No documents match \"$searchQuery\"."
                                else -> "No documents in this collection yet."
                            },
                            modifier = Modifier.padding(32.dp),
                        )
                    }
                }

                viewMode == LibraryViewMode.GRID -> {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(minSize = thumbnailScale.tileWidth),
                        contentPadding = PaddingValues(16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(items, key = { it.id }) { doc ->
                            DocumentGridItem(
                                doc = doc,
                                onOpen = { onOpen(doc.id) },
                                onDelete = { onDelete(doc.id) },
                                onAddToCollection = { onAddToCollection(doc.id) },
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items, key = { it.id }) { doc ->
                            DocumentCard(
                                doc = doc,
                                onOpen = { onOpen(doc.id) },
                                onDelete = { onDelete(doc.id) },
                                onAddToCollection = { onAddToCollection(doc.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LibraryHeader(
    viewMode: LibraryViewMode,
    sortOrder: LibrarySortOrder,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSetViewMode: (LibraryViewMode) -> Unit,
    onSetSortOrder: (LibrarySortOrder) -> Unit,
    onShowImportDialog: () -> Unit,
    collections: List<Collection>,
    selectedCollectionId: Long?,
    onSelectCollection: (Long?) -> Unit,
    onCreateCollection: () -> Unit,
    onDeleteCollection: (Long) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(
                onClick = { onSetViewMode(if (viewMode == LibraryViewMode.LIST) LibraryViewMode.GRID else LibraryViewMode.LIST) },
            ) {
                if (viewMode == LibraryViewMode.LIST) {
                    Icon(Icons.Filled.GridView, contentDescription = "Switch to grid view")
                } else {
                    Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Switch to list view")
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                var showMenu by remember { mutableStateOf(false) }
                Box {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, contentDescription = "More options")
                    }
                    DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                        SortMenuItem("Title (A–Z)", sortOrder == LibrarySortOrder.TITLE) {
                            onSetSortOrder(LibrarySortOrder.TITLE)
                            showMenu = false
                        }
                        SortMenuItem("Date added", sortOrder == LibrarySortOrder.DATE_ADDED) {
                            onSetSortOrder(LibrarySortOrder.DATE_ADDED)
                            showMenu = false
                        }
                        SortMenuItem("Last opened", sortOrder == LibrarySortOrder.LAST_OPENED) {
                            onSetSortOrder(LibrarySortOrder.LAST_OPENED)
                            showMenu = false
                        }
                        SortMenuItem("Progress", sortOrder == LibrarySortOrder.PROGRESS) {
                            onSetSortOrder(LibrarySortOrder.PROGRESS)
                            showMenu = false
                        }
                    }
                }
                Spacer(modifier = Modifier.width(4.dp))
                IconButton(
                    onClick = onShowImportDialog,
                    modifier = Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Filled.Add, contentDescription = "Import text", tint = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }

        Text(
            text = "Library",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = { Text("Search your library…") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        )

        Spacer(modifier = Modifier.height(4.dp))

        CollectionsRow(
            collections = collections,
            selectedCollectionId = selectedCollectionId,
            onSelect = onSelectCollection,
            onCreateNew = onCreateCollection,
            onRequestDelete = { onDeleteCollection(it.id) },
        )
    }
}

@Composable
private fun SortMenuItem(label: String, selected: Boolean, onClick: () -> Unit) {
    DropdownMenuItem(
        text = { Text(label) },
        onClick = onClick,
        trailingIcon = if (selected) {
            { Icon(Icons.Filled.Check, contentDescription = null) }
        } else {
            null
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CollectionsRow(
    collections: List<Collection>,
    selectedCollectionId: Long?,
    onSelect: (Long?) -> Unit,
    onCreateNew: () -> Unit,
    onRequestDelete: (Collection) -> Unit,
) {
    var pendingDelete by remember { mutableStateOf<Collection?>(null) }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            FilterChip(selected = selectedCollectionId == null, onClick = { onSelect(null) }, label = { Text("All") })
        }
        items(collections, key = { it.id }) { collection ->
            FilterChip(
                selected = selectedCollectionId == collection.id,
                onClick = { onSelect(collection.id) },
                label = { Text(collection.name) },
                trailingIcon = {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = "Delete ${collection.name}",
                        modifier = Modifier.size(16.dp).clickable { pendingDelete = collection },
                    )
                },
            )
        }
        item {
            AssistChip(onClick = onCreateNew, label = { Text("+ New") })
        }
    }

    pendingDelete?.let { collection ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete \"${collection.name}\"?") },
            text = { Text("Books stay in your library — they're just removed from this collection.") },
            confirmButton = {
                TextButton(onClick = { onRequestDelete(collection); pendingDelete = null }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun ScaleRow(current: ThumbnailScale, onSet: (ThumbnailScale) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.End,
    ) {
        ThumbnailScale.entries.forEach { scale ->
            FilterChip(
                selected = current == scale,
                onClick = { onSet(scale) },
                label = { Text(scale.shortLabel) },
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

private val ThumbnailScale.shortLabel: String
    get() = when (this) {
        ThumbnailScale.SMALL -> "S"
        ThumbnailScale.MEDIUM -> "M"
        ThumbnailScale.LARGE -> "L"
    }

private val ThumbnailScale.tileWidth: Dp
    get() = when (this) {
        ThumbnailScale.SMALL -> 100.dp
        ThumbnailScale.MEDIUM -> 140.dp
        ThumbnailScale.LARGE -> 180.dp
    }

@Composable
private fun DocumentContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    onAddToCollection: () -> Unit,
    onRequestDelete: () -> Unit,
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismiss) {
        DropdownMenuItem(
            text = { Text("Add to collection…") },
            onClick = { onDismiss(); onAddToCollection() },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = { onDismiss(); onRequestDelete() },
        )
    }
}

@Composable
private fun DeleteConfirmDialog(title: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete \"$title\"?") },
        text = { Text("This can't be undone.") },
        confirmButton = {
            TextButton(onClick = { onDismiss(); onConfirm() }) { Text("Delete") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentCard(doc: DocumentSummary, onOpen: () -> Unit, onDelete: () -> Unit, onAddToCollection: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .combinedClickable(onClick = onOpen, onLongClick = { showMenu = true })
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BookCover(
                title = doc.title,
                coverPath = doc.coverPath,
                modifier = Modifier.size(width = 64.dp, height = 88.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!doc.author.isNullOrBlank()) {
                    Text(
                        text = doc.author,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FormatBadge(doc.format)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = relativeTime(doc.updatedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(modifier = Modifier.width(8.dp))
            CircularProgressRing(progress = doc.progress)
        }
        DocumentContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onAddToCollection = onAddToCollection,
            onRequestDelete = { confirmDelete = true },
        )
    }

    if (confirmDelete) {
        DeleteConfirmDialog(title = doc.title, onConfirm = onDelete, onDismiss = { confirmDelete = false })
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DocumentGridItem(doc: DocumentSummary, onOpen: () -> Unit, onDelete: () -> Unit, onAddToCollection: () -> Unit) {
    var showMenu by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box {
        Column(
            modifier = Modifier.combinedClickable(onClick = onOpen, onLongClick = { showMenu = true }),
        ) {
            BookCover(
                title = doc.title,
                coverPath = doc.coverPath,
                modifier = Modifier.fillMaxWidth().aspectRatio(0.7f).clip(RoundedCornerShape(8.dp)),
            )
            Text(
                text = doc.title,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 6.dp),
            )
            if (!doc.author.isNullOrBlank()) {
                Text(
                    text = doc.author,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            LinearProgressIndicator(
                progress = { doc.progress },
                modifier = Modifier.fillMaxWidth().height(3.dp).padding(top = 4.dp),
            )
        }
        DocumentContextMenu(
            expanded = showMenu,
            onDismiss = { showMenu = false },
            onAddToCollection = onAddToCollection,
            onRequestDelete = { confirmDelete = true },
        )
    }

    if (confirmDelete) {
        DeleteConfirmDialog(title = doc.title, onConfirm = onDelete, onDismiss = { confirmDelete = false })
    }
}
