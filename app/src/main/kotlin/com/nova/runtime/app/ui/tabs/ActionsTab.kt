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
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.runtime.app.ui.NovaOsViewModel
import com.nova.runtime.app.ui.components.ContentDetailState
import com.nova.runtime.app.ui.components.NovaButton
import com.nova.runtime.app.ui.components.NovaButtonVariant
import com.nova.runtime.app.ui.components.NovaCard
import com.nova.runtime.app.ui.components.NovaStatusChip
import com.nova.runtime.app.ui.models.AutomationTaskItem
import com.nova.runtime.app.ui.models.LiveTelemetryState
import com.nova.runtime.app.ui.theme.NovaAccentBlue
import com.nova.runtime.app.ui.theme.NovaPureBlack
import com.nova.runtime.app.ui.theme.NovaSecondaryGrey
import com.nova.runtime.app.ui.theme.NovaSurfaceDim
import com.nova.runtime.app.ui.theme.NovaSurfaceLowest

/**
 * Connected app controls and queued tasks.
 * Driven 100% by real AccessibilityServiceBridge & WorkManager task queues.
 */
@Composable
fun ActionsTab(
    viewModel: NovaOsViewModel,
    telemetryState: LiveTelemetryState = LiveTelemetryState(),
    onExpandContent: (ContentDetailState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val isAccessibilityConn by viewModel.accessibilityConnected.collectAsState()
    val nodeTree by viewModel.accessibilityTreeNodes.collectAsState()
    val tasksQueue by viewModel.automationQueueTasks.collectAsState()
    var newTaskText by remember { mutableStateOf("") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        // Header
        Column {
            Text(
                text = "Actions",
                style =
                    MaterialTheme.typography.headlineLarge.copy(
                        fontWeight = FontWeight.Bold,
                        letterSpacing = (-0.02).sp,
                    ),
                color = NovaPureBlack,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(NovaSurfaceLowest, RoundedCornerShape(16.dp))
                        .padding(8.dp),
            ) {
                Text(
                    text = "Manage tasks and connected app controls.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaSecondaryGrey,
                )
            }
        }

        // Accessibility Node Graph Card
        NovaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NovaSurfaceLowest,
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "App controls",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaPureBlack,
                        )
                        Text(
                            text = if (isAccessibilityConn) "Service connected" else "Service disconnected",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isAccessibilityConn) NovaAccentBlue else NovaSecondaryGrey,
                        )
                    }

                    NovaStatusChip(
                        status = if (isAccessibilityConn) "CONNECTED" else "STANDBY",
                        isPrimary = isAccessibilityConn,
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Node Tree Display List
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
                            .background(NovaSurfaceLowest, RoundedCornerShape(16.dp))
                            .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (nodeTree.isEmpty()) {
                        Text(
                            text = if (isAccessibilityConn) "No controls found in the current window." else "Connect NOVA�s accessibility service to inspect app controls.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = NovaSecondaryGrey,
                        )
                    } else {
                        nodeTree.take(8).forEach { nodeStr ->
                            TreeItemRow(nodeStr)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                NovaButton(
                    text = "View app controls",
                    onClick = {
                        onExpandContent(
                            ContentDetailState(
                                fileName = "ACCESSIBILITY TREE DUMP",
                                text =
                                    "Live Accessibility Window Tree:\n• Service Connected: $isAccessibilityConn\n\nDump:\n" +
                                        (if (nodeTree.isEmpty()) "No active window accessibility nodes found. Enable Nova Accessibility Service in Settings." else nodeTree.joinToString("\n")),
                            ),
                        )
                    },
                    variant = NovaButtonVariant.SECONDARY,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Filled.AccountTree,
                            contentDescription = "Tree",
                            tint = NovaPureBlack,
                            modifier = Modifier.size(16.dp),
                        )
                    },
                )
            }
        }

        // Automation Queue Card
        NovaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NovaSurfaceLowest,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Task queue",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaPureBlack,
                    )
                    Text(
                        text = "${tasksQueue.size} TASKS PENDING",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaAccentBlue,
                    )
                }

                // Add Task Input Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = newTaskText,
                        onValueChange = { newTaskText = it },
                        modifier = Modifier.weight(1f),
                        placeholder = { Text("Enqueue new task...", style = MaterialTheme.typography.bodyMedium) },
                        singleLine = true,
                        colors =
                            OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = NovaAccentBlue,
                                unfocusedBorderColor = NovaPureBlack,
                                focusedContainerColor = NovaSurfaceLowest,
                                unfocusedContainerColor = NovaSurfaceLowest,
                            ),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions =
                            KeyboardActions(
                                onDone = {
                                    if (newTaskText.isNotBlank()) {
                                        viewModel.enqueueNewTask(newTaskText)
                                        newTaskText = ""
                                    }
                                },
                            ),
                    )

                    NovaButton(
                        text = "Add",
                        onClick = {
                            if (newTaskText.isNotBlank()) {
                                viewModel.enqueueNewTask(newTaskText)
                                newTaskText = ""
                            }
                        },
                        variant = NovaButtonVariant.PRIMARY,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add",
                                tint = NovaSurfaceLowest,
                                modifier = Modifier.size(16.dp),
                            )
                        },
                    )
                }

                // Tasks List
                tasksQueue.forEach { task ->
                    TaskItemRow(
                        task = task,
                        onExpandContent = onExpandContent,
                    )
                }
            }
        }

        // Execution Latency & Safety Card
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            NovaCard(
                modifier = Modifier.weight(1f),
                backgroundColor = NovaSurfaceLowest,
            ) {
                Column {
                    Text(
                        text = "Last command",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaSecondaryGrey,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = telemetryState.lastCommandLatencyMs?.let { "$it ms" } ?: "Not measured",
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaPureBlack,
                    )
                }
            }

            NovaCard(
                modifier = Modifier.weight(1f),
                backgroundColor = NovaSurfaceLowest,
            ) {
                Column {
                    Text(
                        text = "Action review",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        color = NovaSecondaryGrey,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Safe",
                            tint = NovaAccentBlue,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Enabled",
                            style =
                                MaterialTheme.typography.headlineMedium.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 18.sp,
                                ),
                            color = NovaPureBlack,
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun TreeItemRow(text: String) {
    Text(
        text = text,
        style =
            MaterialTheme.typography.bodySmall.copy(
                fontFamily = androidx.compose.ui.text.font.FontFamily.SansSerif,
                fontSize = 11.sp,
            ),
        color = NovaPureBlack,
    )
}

@Composable
private fun TaskItemRow(
    task: AutomationTaskItem,
    onExpandContent: (ContentDetailState) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
                .clickable {
                    onExpandContent(
                        ContentDetailState(
                            fileName = "TASK #${task.id}: ${task.title}",
                            text = "Task Details:\n• ID: ${task.id}\n• Title: ${task.title}\n• Status: ${task.status}\n• Progress: ${task.progressPercent}%\n• Dispatcher: WorkManager Background Thread",
                        ),
                    )
                }
                .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${task.indexLabel}.",
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaAccentBlue,
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaPureBlack,
                )
            }

            NovaStatusChip(
                status = task.status,
                isPrimary = task.status == "RUNNING" || task.status == "DONE",
            )
        }

        if (task.status == "RUNNING") {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { task.progressPercent / 100f },
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                color = NovaAccentBlue,
                trackColor = NovaSurfaceDim,
            )
        }
    }
}
