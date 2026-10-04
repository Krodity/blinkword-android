package uk.krodity.blinkword.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import uk.krodity.blinkword.data.AppTheme
import uk.krodity.blinkword.data.tts.NeuralModel
import uk.krodity.blinkword.data.tts.TtsVoice
import uk.krodity.blinkword.ui.NeuralTtsState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheet(
    theme: AppTheme,
    fontSizeSp: Float,
    showAnchorHighlight: Boolean,
    showFocusGuides: Boolean,
    onDismiss: () -> Unit,
    onThemeChange: (AppTheme) -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onShowAnchorHighlightChange: (Boolean) -> Unit,
    onShowFocusGuidesChange: (Boolean) -> Unit,
    onExportBackup: (Uri) -> Unit,
    onImportBackup: (Uri) -> Unit,
    voices: List<TtsVoice>,
    selectedVoice: String?,
    onVoiceChange: (String?) -> Unit,
    neuralState: NeuralTtsState,
    useNeural: Boolean,
    neuralModelId: String?,
    neuralSpeakerId: Int,
    onToggleNeural: (Boolean) -> Unit,
    onDownloadModel: (NeuralModel) -> Unit,
    onDeleteModel: (String) -> Unit,
    onSelectNeuralVoice: (String, Int) -> Unit,
    onPreviewNeuralVoice: (String, Int) -> Unit,
) {
    val exportPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(onExportBackup) }

    val importPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(onImportBackup) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Theme")
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                AppTheme.entries.forEach { option ->
                    FilterChip(
                        selected = option == theme,
                        onClick = { onThemeChange(option) },
                        label = { Text(option.name.lowercase().replaceFirstChar { it.uppercase() }) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
            Text("Font size (${fontSizeSp.toInt()}sp)")
            Slider(
                value = fontSizeSp,
                onValueChange = onFontSizeChange,
                valueRange = 24f..96f,
            )
            Text(
                text = "Sample",
                fontSize = fontSizeSp.sp,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Anchor letter highlight")
                Switch(checked = showAnchorHighlight, onCheckedChange = onShowAnchorHighlightChange)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text("Focus guides")
                Switch(checked = showFocusGuides, onCheckedChange = onShowFocusGuidesChange)
            }

            Spacer(modifier = Modifier.height(24.dp))
            NeuralVoiceSection(
                state = neuralState,
                useNeural = useNeural,
                selectedModelId = neuralModelId,
                selectedSpeakerId = neuralSpeakerId,
                onToggleNeural = onToggleNeural,
                onDownloadModel = onDownloadModel,
                onDeleteModel = onDeleteModel,
                onSelectVoice = onSelectNeuralVoice,
                onPreviewVoice = onPreviewNeuralVoice,
            )

            Spacer(modifier = Modifier.height(24.dp))
            Text("System voice")
            Spacer(modifier = Modifier.height(4.dp))
            VoicePicker(voices = voices, selectedVoice = selectedVoice, onVoiceChange = onVoiceChange)

            Spacer(modifier = Modifier.height(24.dp))
            Text("Backup")
            Text(
                text = "Saves your library list, reading positions, collections and stats — " +
                    "not the book text. Anything from Discover is re-downloaded on restore.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { exportPicker.launch("blinkword-backup.json") },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Export")
                }
                OutlinedButton(
                    onClick = { importPicker.launch(arrayOf("application/json", "text/plain")) },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Restore")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun VoicePicker(voices: List<TtsVoice>, selectedVoice: String?, onVoiceChange: (String?) -> Unit) {
    var expanded by remember { mutableStateOf(false) }

    if (voices.isEmpty()) {
        Text(
            text = "No speech voices installed on this device.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }

    val current = voices.firstOrNull { it.name == selectedVoice }

    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
            Text(current?.label ?: "Device default", maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.heightIn(max = 360.dp),
        ) {
            DropdownMenuItem(
                text = { Text("Device default") },
                onClick = {
                    expanded = false
                    onVoiceChange(null)
                },
                trailingIcon = if (current == null) {
                    { Icon(Icons.Filled.Check, contentDescription = null) }
                } else {
                    null
                },
            )

            voices.forEach { voice ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = if (voice.requiresNetwork) "${voice.label} (online)" else voice.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    },
                    onClick = {
                        expanded = false
                        onVoiceChange(voice.name)
                    },
                    trailingIcon = if (voice.name == selectedVoice) {
                        { Icon(Icons.Filled.Check, contentDescription = null) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}
