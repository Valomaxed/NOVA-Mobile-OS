package com.nova.runtime.app.ui.tabs

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.runtime.app.ui.NovaOsViewModel
import com.nova.runtime.app.ui.components.ContentDetailState
import com.nova.runtime.app.ui.components.NovaButton
import com.nova.runtime.app.ui.components.NovaButtonVariant
import com.nova.runtime.app.ui.components.NovaCard
import com.nova.runtime.app.ui.models.LiveTelemetryState
import com.nova.runtime.app.ui.theme.NovaAccentBlue
import com.nova.runtime.app.ui.theme.NovaPureBlack
import com.nova.runtime.app.ui.theme.NovaSecondaryGrey
import com.nova.runtime.storage.dao.DocumentLibraryEntry
import java.util.UUID

/** A lazily composed library with metadata-only rows and on-demand document bodies. */
@Composable
fun DocumentsTab(
    viewModel: NovaOsViewModel,
    indexingStatus: String?,
    telemetryState: LiveTelemetryState = LiveTelemetryState(),
    onExpandContent: (ContentDetailState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val totalDocuments by viewModel.totalDocumentsCount.collectAsStateWithLifecycle()
    val documents by viewModel.indexedDocumentsList.collectAsStateWithLifecycle()
    val dailyDocuments by viewModel.dailyDocuments.collectAsStateWithLifecycle()
    var importDaily by rememberSaveable { mutableStateOf(false) }
    var askFile by rememberSaveable { mutableStateOf<String?>(null) }
    var dailyQuestion by rememberSaveable { mutableStateOf("") }
    val processing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val importing by viewModel.isImportingDocument.collectAsStateWithLifecycle()
    val message by viewModel.documentLibraryMessage.collectAsStateWithLifecycle()
    var removeId by rememberSaveable { mutableStateOf<String?>(null) }
    var removeName by rememberSaveable { mutableStateOf("") }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        uri?.let { viewModel.ingestDocumentFromUri(context, it, dailyEssential = importDaily) }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item(key = "header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Documents", style = MaterialTheme.typography.headlineLarge, color = NovaPureBlack)
                Text(
                    "Import a file to make its contents available to local search.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaSecondaryGrey,
                )
            }
        }
        item(key = "daily-heading", contentType = "daily-heading") {
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Daily essentials", style = MaterialTheme.typography.titleLarge)
                    Text("Keep your timetable, menu, routines, and other frequently used documents here. Ask about a specific file to keep the answer focused on that source.", style = MaterialTheme.typography.bodyMedium)
                    NovaButton(
                        text = "Add daily document",
                        onClick = { importDaily = true; picker.launch(arrayOf("*/*")) },
                        enabled = !importing,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (dailyDocuments.isEmpty()) Text("No daily documents saved yet.", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
        items(dailyDocuments, key = { "daily-${it.id}" }, contentType = { "daily-document" }) { document ->
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(document.name, style = MaterialTheme.typography.titleMedium)
                    Text(if (document.contentExtractStatus == "SUCCESS") "Ready to ask" else "Open the file to check extracted text before asking.", style = MaterialTheme.typography.bodySmall)
                    TextButton(onClick = { viewModel.openLibraryDocument(document.id, onExpandContent) }) { Text("Review extracted text") }
                    NovaButton(text = "Ask about this file", onClick = { askFile = document.name; dailyQuestion = "" }, enabled = !processing && document.contentExtractStatus == "SUCCESS")
                    TextButton(onClick = { viewModel.setDailyDocument(document.id, false) }) { Text("Remove from daily essentials") }
                }
            }
        }
        item(key = "metrics", contentType = "metrics") {
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("$totalDocuments documents", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${telemetryState.totalDocumentsIndexed} searchable · ${telemetryState.indexedVectorCount} stored vectors",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaSecondaryGrey,
                    )
                    Text(
                        telemetryState.ragLatencyMs?.let { "Last vector search: %.2f ms".format(it) }
                            ?: "Search latency has not been measured yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = NovaSecondaryGrey,
                    )
                }
            }
        }
        item(key = "import", contentType = "import") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                NovaButton(
                    text = if (importing) "Importing…" else "Import document",
                    onClick = { importDaily = false; picker.launch(arrayOf("*/*")) },
                    enabled = !importing,
                    modifier = Modifier.fillMaxWidth(),
                )
                (message ?: indexingStatus)?.let {
                    Text(
                        it,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaSecondaryGrey,
                    )
                }
            }
        }
        if (documents.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                NovaCard(modifier = Modifier.fillMaxWidth()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            if (totalDocuments == 0) "No documents yet" else "Loading documents…",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            "Imported files appear here. Tap a document to read its extracted text.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaSecondaryGrey,
                        )
                    }
                }
            }
        } else {
            item(key = "list-heading", contentType = "heading") {
                Text("Recent documents", style = MaterialTheme.typography.titleMedium)
            }
            items(documents, key = { it.id.toString() }, contentType = { "document" }) { document ->
                if (dailyDocuments.none { it.id == document.id }) {
                    TextButton(onClick = { viewModel.setDailyDocument(document.id, true) }) { Text("Save ${document.name} to daily essentials") }
                }
                DocumentLibraryRow(
                    document = document,
                    canRemove = !importing,
                    onOpen = { viewModel.openLibraryDocument(document.id, onExpandContent) },
                    onRemove = {
                        removeId = document.id.toString()
                        removeName = document.name
                    },
                )
            }
        }
        item(key = "footer", contentType = "footer") {
            Column(modifier = Modifier.padding(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Showing ${documents.size} of $totalDocuments",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaSecondaryGrey,
                )
                if (documents.size < totalDocuments) {
                    NovaButton(
                        text = "Load more documents",
                        onClick = viewModel::loadMoreDocuments,
                        modifier = Modifier.fillMaxWidth(),
                        variant = NovaButtonVariant.OUTLINE,
                    )
                }
            }
        }
    }

    askFile?.let { name ->
        AlertDialog(
            onDismissRequest = { askFile = null },
            title = { Text("Ask about $name") },
            text = {
                OutlinedTextField(value = dailyQuestion, onValueChange = { dailyQuestion = it }, label = { Text("Your question") })
            },
            confirmButton = {
                TextButton(enabled = dailyQuestion.isNotBlank() && !processing, onClick = {
                    val quote = 34.toChar()
                    viewModel.submitCommand(dailyQuestion.trim() + " in " + quote + name.replace(quote.toString(), "") + quote)
                    viewModel.selectTab(com.nova.runtime.app.ui.components.NovaScreenTab.COMMAND)
                    askFile = null
                }) { Text("Ask NOVA") }
            },
            dismissButton = { TextButton(onClick = { askFile = null }) { Text("Cancel") } },
        )
    }
    removeId?.let { id ->
        AlertDialog(
            onDismissRequest = { removeId = null },
            title = { Text("Remove document?") },
            text = { Text("Remove $removeName from NOVA's library? The original file will stay on your device.") },
            confirmButton = {
                TextButton(enabled = !importing, onClick = {
                    viewModel.removeLibraryDocument(UUID.fromString(id))
                    removeId = null
                }) { Text("Remove", color = NovaAccentBlue) }
            },
            dismissButton = { TextButton(onClick = { removeId = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DocumentLibraryRow(
    document: DocumentLibraryEntry,
    canRemove: Boolean,
    onOpen: () -> Unit,
    onRemove: () -> Unit,
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.weight(1f).clickable(onClick = onOpen).padding(vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    document.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                val status = when {
                    document.hasVector -> "Searchable"
                    document.contentExtractStatus == "FAILED" -> "Text extraction failed"
                    document.contentExtractStatus == "EMPTY" -> "No text found"
                    else -> "Search indexing pending"
                }
                Text(
                    "$status · ${document.size} bytes",
                    style = MaterialTheme.typography.bodySmall,
                    color = NovaSecondaryGrey,
                )
            }
            IconButton(onClick = onRemove, enabled = canRemove) {
                Icon(Icons.Filled.Delete, contentDescription = "Remove ${document.name} from library", tint = NovaAccentBlue)
            }
        }
        HorizontalDivider()
    }
}
