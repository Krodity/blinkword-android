package uk.krodity.blinkword.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import uk.krodity.blinkword.data.tts.NeuralModel
import uk.krodity.blinkword.data.tts.NeuralVoiceCatalogue
import uk.krodity.blinkword.ui.NeuralTtsState

@Composable
fun NeuralVoiceSection(
    state: NeuralTtsState,
    useNeural: Boolean,
    selectedModelId: String?,
    selectedSpeakerId: Int,
    onToggleNeural: (Boolean) -> Unit,
    onDownloadModel: (NeuralModel) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSelectVoice: (String, Int) -> Unit,
    onPreviewVoice: (String, Int) -> Unit,
) {
    val anyInstalled = state.installedIds.isNotEmpty()

    Text("Neural voices")
    Text(
        text = "Much better quality and still fully offline, in exchange for a one-time download.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )

    Spacer(modifier = Modifier.height(8.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = if (anyInstalled) "Use neural voices" else "Use neural voices (download one first)",
            color = if (anyInstalled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Switch(checked = useNeural && anyInstalled, onCheckedChange = onToggleNeural, enabled = anyInstalled)
    }

    Spacer(modifier = Modifier.height(4.dp))

    NeuralVoiceCatalogue.models.forEach { model ->
        val installed = model.id in state.installedIds
        val downloading = state.downloadingId == model.id

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(model.displayName, style = MaterialTheme.typography.bodyLarge)
                Text(
                    text = "${model.description} · ${model.approxBytes / 1_000_000} MB",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (downloading) {
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { state.progress },
                        modifier = Modifier.fillMaxWidth().height(4.dp),
                    )
                }
            }

            when {
                downloading -> CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)

                installed -> IconButton(onClick = { onDeleteModel(model.id) }) {
                    Icon(Icons.Filled.Delete, contentDescription = "Remove ${model.displayName}")
                }

                else -> IconButton(
                    onClick = { onDownloadModel(model) },
                    enabled = state.downloadingId == null,
                ) {
                    Icon(Icons.Filled.Download, contentDescription = "Download ${model.displayName}")
                }
            }
        }

        if (installed) {
            InstalledModelVoices(
                model = model,
                isActiveModel = selectedModelId == model.id,
                selectedSpeakerId = selectedSpeakerId,
                // Only the active model's speaker choice is meaningful; others preview voice 0.
                speakerCount = state.speakerCounts[model.id] ?: model.speakers.size.coerceAtLeast(1),
                previewing = state.previewingId == model.id,
                onSelectVoice = onSelectVoice,
                onPreviewVoice = onPreviewVoice,
            )
        }
    }
}

@Composable
private fun InstalledModelVoices(
    model: NeuralModel,
    isActiveModel: Boolean,
    selectedSpeakerId: Int,
    speakerCount: Int,
    previewing: Boolean,
    onSelectVoice: (String, Int) -> Unit,
    onPreviewVoice: (String, Int) -> Unit,
) {
    val labels = NeuralVoiceCatalogue.speakerLabels(model, speakerCount)
    val speakerForPreview = if (isActiveModel) selectedSpeakerId else 0
    var expanded by remember { mutableStateOf(false) }

    if (labels.size <= 1) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { onSelectVoice(model.id, 0) },
                enabled = !isActiveModel,
                modifier = Modifier.weight(1f),
            ) {
                Text(if (isActiveModel) "Selected" else "Use this voice")
            }
            PreviewButton(previewing) { onPreviewVoice(model.id, 0) }
        }
        return
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
    Box(modifier = Modifier.weight(1f)) {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            val current = labels.getOrNull(selectedSpeakerId)
            Text(
                text = if (isActiveModel && current != null) current else "Choose a voice",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            labels.forEachIndexed { index, label ->
                DropdownMenuItem(
                    text = { Text(label) },
                    onClick = {
                        expanded = false
                        onSelectVoice(model.id, index)
                    },
                    trailingIcon = if (isActiveModel && index == selectedSpeakerId) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
        PreviewButton(previewing) { onPreviewVoice(model.id, speakerForPreview) }
    }
}

@Composable
private fun PreviewButton(previewing: Boolean, onClick: () -> Unit) {
    OutlinedButton(onClick = onClick, enabled = !previewing) {
        if (previewing) {
            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        } else {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
        }
    }
}
