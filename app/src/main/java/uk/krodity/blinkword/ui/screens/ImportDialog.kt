package uk.krodity.blinkword.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportDialog(
    onDismiss: () -> Unit,
    onImportPastedText: (title: String, body: String) -> Unit,
    onImportFromUri: (Uri) -> Unit,
) {
    var title by remember { mutableStateOf("") }
    var body by remember { mutableStateOf("") }

    val filePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let(onImportFromUri)
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text("Import text")
            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Title") },
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = body,
                onValueChange = { body = it },
                label = { Text("Paste text") },
                modifier = Modifier.fillMaxWidth().height(160.dp),
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { onImportPastedText(title, body) },
                enabled = body.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Import pasted text")
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("or")
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = {
                    filePicker.launch(arrayOf("text/*", "application/epub+zip", "application/pdf"))
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Choose a file… (.txt, .md, .epub, .pdf)")
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
