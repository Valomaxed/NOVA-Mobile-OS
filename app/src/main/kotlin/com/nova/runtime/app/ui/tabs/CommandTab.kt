package com.nova.runtime.app.ui.tabs

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.runtime.app.ui.NovaOsViewModel
import com.nova.runtime.app.ui.components.ContentDetailState
import com.nova.runtime.app.ui.components.NovaButton
import com.nova.runtime.app.ui.components.NovaButtonVariant
import com.nova.runtime.app.ui.components.NovaCard
import com.nova.runtime.app.ui.components.NovaStatusChip
import com.nova.runtime.app.ui.models.CommandLogEntry
import com.nova.runtime.app.ui.theme.NovaAccentBlue
import com.nova.runtime.app.ui.theme.NovaPureBlack
import com.nova.runtime.app.ui.theme.NovaSecondaryGrey
import com.nova.runtime.app.ui.theme.NovaSurfaceDim
import com.nova.runtime.app.ui.theme.NovaSurfaceLowest

/**
 * Requests and results using the shared minimal theme.
 * Driven 100% by real CognitivePipelineOrchestrator and Command Log stream.
 */
@Composable
fun CommandTab(
    viewModel: NovaOsViewModel,
    onMicToggle: () -> Unit = {},
    onExpandContent: (ContentDetailState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val commandLog by viewModel.commandHistory.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    var promptInputText by rememberSaveable { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Column {
            Text(
                text = "Ask NOVA",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-0.02).sp,
                ),
                color = NovaPureBlack,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(NovaSurfaceLowest, RoundedCornerShape(16.dp))
                    .padding(8.dp),
            ) {
                Text(
                    text = "Use a command or your voice. Review the result below.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaSecondaryGrey,
                )
            }
        }

        // Command Prompt Input Card
        NovaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NovaSurfaceLowest,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Your request",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaPureBlack,
                    )
                    NovaStatusChip(
                        status = if (isProcessing) "EXECUTING..." else "READY",
                        isPrimary = isProcessing,
                    )
                }

                OutlinedTextField(
                    value = promptInputText,
                    onValueChange = { promptInputText = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("What would you like to do?") },
                    placeholder = {
                        Text(
                            text = "Ask a question or describe a task�",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = NovaAccentBlue,
                        unfocusedBorderColor = NovaPureBlack,
                        focusedContainerColor = NovaSurfaceLowest,
                        unfocusedContainerColor = NovaSurfaceLowest,
                    ),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(
                        onSend = {
                            if (promptInputText.isNotBlank() && !isProcessing && !isRecordingVoice) {
                                viewModel.submitCommand(promptInputText)
                                promptInputText = ""
                            }
                        }
                    ),
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    NovaButton(
                        text = if (isRecordingVoice) "Stop recording" else "Use voice",
                        onClick = onMicToggle,
                        modifier = Modifier.weight(1f),
                        enabled = !isProcessing,
                        variant = if (isRecordingVoice) NovaButtonVariant.PRIMARY_RED else NovaButtonVariant.SECONDARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Mic,
                                contentDescription = null,
                                tint = if (isRecordingVoice) NovaSurfaceLowest else NovaPureBlack,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )

                    NovaButton(
                        text = "Send request",
                        enabled = promptInputText.isNotBlank() && !isProcessing && !isRecordingVoice,
                        onClick = {
                            if (promptInputText.isNotBlank() && !isProcessing && !isRecordingVoice) {
                                viewModel.submitCommand(promptInputText)
                                promptInputText = ""
                            }
                        },
                        variant = NovaButtonVariant.PRIMARY,
                        modifier = Modifier.weight(1f),
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Send,
                                contentDescription = null,
                                tint = NovaSurfaceLowest,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }
        }

        // Execution History Stream Header
        Text(
            text = "Command history",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            color = NovaPureBlack,
        )

        // Command Log Entries
        if (commandLog.isEmpty()) {
            NovaCard(
                modifier = Modifier.fillMaxWidth(),
                backgroundColor = NovaSurfaceLowest,
            ) {
                Text(
                    text = "No command logs recorded yet. Submit a prompt above to trigger execution.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = NovaSecondaryGrey,
                )
            }
        } else {
            commandLog.forEach { entry ->
                CommandLogItemCard(
                    entry = entry,
                    onExpandContent = onExpandContent,
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun CommandLogItemCard(
    entry: CommandLogEntry,
    onExpandContent: (ContentDetailState) -> Unit,
) {
    NovaCard(
        modifier = Modifier.fillMaxWidth(),
        backgroundColor = NovaSurfaceLowest,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "#${entry.indexLabel}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaAccentBlue,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = entry.timestamp,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaSecondaryGrey,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${entry.latencyMs}ms",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaPureBlack,
                    )
                }

                NovaStatusChip(
                    status = entry.status,
                    isPrimary = entry.status == "RESOLVED",
                    isSecondary = entry.status != "RESOLVED",
                )
            }

            // Input Prompt Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
                    .background(NovaSurfaceDim, RoundedCornerShape(16.dp))
                    .padding(10.dp),
            ) {
                Column {
                    Text(
                        text = "Your request",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                        color = NovaSecondaryGrey,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.commandText,
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaPureBlack,
                    )
                }
            }

            // Response Output Block
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(width = 1.dp, color = NovaAccentBlue, shape = RoundedCornerShape(16.dp))
                    .background(NovaSurfaceLowest, RoundedCornerShape(16.dp))
                    .padding(10.dp),
            ) {
                Column {
                    Text(
                        text = "NOVA�s answer",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium, fontSize = 12.sp),
                        color = NovaAccentBlue,
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = entry.responseText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaPureBlack,
                    )
                }
            }

            // Tags & Expand Details Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    entry.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold),
                                color = NovaPureBlack,
                            )
                        }
                    }
                }

                Text(
                    text = "View details",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = NovaAccentBlue,
                    ),
                    modifier = Modifier.clickable {
                        onExpandContent(
                            ContentDetailState(
                                fileName = "COMMAND LOG #${entry.indexLabel}",
                                text = "Command Log Trace Details:\n• ID: ${entry.id}\n• Timestamp: ${entry.timestamp}\n• Latency: ${entry.latencyMs} ms\n• Status: ${entry.status}\n\nCommand:\n${entry.commandText}\n\nResponse Summary:\n${entry.responseText}",
                            )
                        )
                    },
                )
            }
        }
    }
}
