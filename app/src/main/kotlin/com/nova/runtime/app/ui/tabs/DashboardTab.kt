package com.nova.runtime.app.ui.tabs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.runtime.app.ui.NovaOsViewModel
import com.nova.runtime.app.ui.components.ActivityCard
import com.nova.runtime.app.ui.components.ActivityItem
import com.nova.runtime.app.ui.components.ContentDetailState
import com.nova.runtime.app.ui.components.NovaButton
import com.nova.runtime.app.ui.components.NovaButtonVariant
import com.nova.runtime.app.ui.components.NovaCard
import com.nova.runtime.app.ui.components.NovaScreenTab
import com.nova.runtime.app.ui.components.NovaStatusChip
import com.nova.runtime.app.ui.models.LiveTelemetryState
import com.nova.runtime.app.ui.theme.NovaColors
import com.nova.runtime.models.RuntimeLifecycleState

/** Overview of runtime state, controls, and actual activity; no simulated hardware readings. */
@Composable
fun DashboardTab(
    viewModel: NovaOsViewModel,
    lifecycleState: RuntimeLifecycleState,
    indexingStatus: String?,
    activityFeed: List<ActivityItem>,
    telemetryState: LiveTelemetryState = LiveTelemetryState(),
    onExpandContent: (ContentDetailState) -> Unit,
    modifier: Modifier = Modifier,
) {
    val nlpEnabled by viewModel.nlpModuleEnabled.collectAsStateWithLifecycle()
    val actionsEnabled by viewModel.actionsModuleEnabled.collectAsStateWithLifecycle()
    val docsEnabled by viewModel.docsModuleEnabled.collectAsStateWithLifecycle()
    val appEnabled by viewModel.appModuleEnabled.collectAsStateWithLifecycle()
    val systemPwrActive by viewModel.systemPwrActive.collectAsStateWithLifecycle()
    val activeLayer by viewModel.activeLayer.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val temperature by viewModel.temperatureSetting.collectAsStateWithLifecycle()
    val contextWindow by viewModel.contextWindowSetting.collectAsStateWithLifecycle()
    val quantization by viewModel.quantizationSetting.collectAsStateWithLifecycle()
    val status = when (lifecycleState) {
        RuntimeLifecycleState.CREATED -> "Starting"
        RuntimeLifecycleState.INITIALIZING -> "Preparing"
        RuntimeLifecycleState.READY -> if (isProcessing) "Working" else "Ready"
        RuntimeLifecycleState.STOPPING -> "Stopping"
        RuntimeLifecycleState.STOPPED -> "Stopped"
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Your local workspace", style = MaterialTheme.typography.headlineLarge, color = NovaColors.textPrimary)
                Text("Ask a question, find a file, or run an action.", style = MaterialTheme.typography.bodyMedium, color = NovaColors.textSecondary)
            }
        }
        item {
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Runtime", style = MaterialTheme.typography.titleMedium, color = NovaColors.textPrimary)
                            Text(activeLayer, style = MaterialTheme.typography.bodyMedium, color = NovaColors.textSecondary)
                        }
                        NovaStatusChip(status = status, isPrimary = isProcessing, isSecondary = !isProcessing)
                    }
                    if (!indexingStatus.isNullOrBlank()) {
                        Text(indexingStatus, style = MaterialTheme.typography.bodyMedium, color = NovaColors.textSecondary)
                    }
                    NovaButton(
                        text = if (isProcessing) "View commands" else "Ask NOVA",
                        onClick = { viewModel.selectTab(NovaScreenTab.COMMAND) },
                        modifier = Modifier.fillMaxWidth(),
                        variant = NovaButtonVariant.PRIMARY_RED,
                    )
                    Text("Session uptime · ${telemetryState.formattedUptime}", style = MaterialTheme.typography.labelMedium, color = NovaColors.textSecondary)
                }
            }
        }
        item {
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column {
                    Text("Runtime controls", style = MaterialTheme.typography.titleMedium, color = NovaColors.textPrimary)
                    Spacer(Modifier.height(8.dp))
                    ModuleRow("System power", "Enable NOVA processing", systemPwrActive) { viewModel.toggleSystemPower() }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    ModuleRow("Language", "Understand requests", nlpEnabled) { viewModel.toggleModule("NLP") }
                    ModuleRow("Actions", "Execute planned tasks", actionsEnabled) { viewModel.toggleModule("ACTIONS") }
                    ModuleRow("Documents", "Search your local knowledge", docsEnabled) { viewModel.toggleModule("DOCS") }
                    ModuleRow("Apps", "Control connected applications", appEnabled) { viewModel.toggleModule("APP") }
                }
            }
        }
        item {
            NovaCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Model configuration", style = MaterialTheme.typography.titleMedium, color = NovaColors.textPrimary)
                    Text("Temperature $temperature · Context ${if (contextWindow > 0) "%,d tokens".format(contextWindow) else "model not installed"}", style = MaterialTheme.typography.bodyMedium, color = NovaColors.textSecondary)
                    ModuleRow("Quantization", "Requested model preference", quantization) { viewModel.toggleQuantization() }
                    NovaButton(text = "Open settings", onClick = { viewModel.selectTab(NovaScreenTab.SETTINGS) }, variant = NovaButtonVariant.SECONDARY)
                }
            }
        }
        item {
            Text("Recent activity", style = MaterialTheme.typography.titleMedium, color = NovaColors.textPrimary)
        }
        if (activityFeed.isEmpty()) {
            item {
                Text("Your requests and results will appear here. Start with Ask NOVA above.", style = MaterialTheme.typography.bodyMedium, color = NovaColors.textSecondary)
            }
        } else {
            items(activityFeed.take(6)) { activity ->
                ActivityCard(item = activity, onExpandContent = onExpandContent)
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun ModuleRow(title: String, description: String, checked: Boolean, onToggle: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = { onToggle() })
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = NovaColors.textPrimary)
            Text(description, style = MaterialTheme.typography.labelMedium, color = NovaColors.textSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(checkedTrackColor = NovaColors.primary),
        )
    }
}
