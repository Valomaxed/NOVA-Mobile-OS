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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nova.runtime.app.ui.NovaOsViewModel
import com.nova.runtime.app.ui.components.ContentDetailState
import com.nova.runtime.app.ui.components.NovaButton
import com.nova.runtime.app.ui.components.NovaButtonVariant
import com.nova.runtime.app.ui.components.NovaCard
import com.nova.runtime.app.ui.components.NovaStatusChip
import com.nova.runtime.app.ui.components.NovaToggle
import com.nova.runtime.app.ui.models.LiveTelemetryState
import com.nova.runtime.app.ui.models.RuntimeModelSpec
import com.nova.runtime.app.ui.theme.NovaAccentBlue
import com.nova.runtime.app.ui.theme.NovaPureBlack
import com.nova.runtime.app.ui.theme.NovaSecondaryGrey
import com.nova.runtime.app.ui.theme.NovaSurfaceDim
import com.nova.runtime.app.ui.theme.NovaSurfaceLowest

/**
 * Model settings and measured runtime status.
 * Driven 100% by real PreferenceDao, ModelDownloadManager, and telemetry state.
 */
@Composable
fun SettingsTab(
    viewModel: NovaOsViewModel,
    telemetryState: LiveTelemetryState = LiveTelemetryState(),
    onExpandContent: (ContentDetailState) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val tempSetting by viewModel.temperatureSetting.collectAsState()
    val ctxSetting by viewModel.contextWindowSetting.collectAsState()
    val quantSetting by viewModel.quantizationSetting.collectAsState()
    val runtimeModels by viewModel.runtimeModelsList.collectAsState()

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
                text = "Settings",
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
                    text = "Model capacity and device information.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = NovaSecondaryGrey,
                )
            }
        }

        // Model Hyperparameters Card
        NovaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NovaSurfaceLowest,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    text = "Model preferences",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = NovaPureBlack,
                )

                // Temperature Slider
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Temperature",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaPureBlack,
                        )
                        Text(
                            text = "%.2f".format(tempSetting),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaAccentBlue,
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Slider(
                        value = tempSetting,
                        onValueChange = { viewModel.updateTemperature(it) },
                        valueRange = 0.0f..1.0f,
                        colors =
                            SliderDefaults.colors(
                                thumbColor = NovaAccentBlue,
                                activeTrackColor = NovaAccentBlue,
                                inactiveTrackColor = NovaSurfaceDim,
                            ),
                    )
                }

                // Context Window Selector Row
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Model context",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaPureBlack,
                        )
                        Text(
                            text = if (ctxSetting > 0) "%,d tokens".format(ctxSetting) else "Not installed",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = NovaPureBlack,
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "The current model sets this limit, including your question, source text, and answer. The new Gemma download supports 4,096 tokens; the earlier bundle supports 2,048.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = NovaSecondaryGrey,
                    )
                }

                // Quantization Toggle Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column {
                        Text(
                            text = "Prefer quantized models",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = NovaPureBlack,
                        )
                        Text(
                            text = "Requires a compatible model bundle",
                            style = MaterialTheme.typography.labelSmall,
                            color = NovaSecondaryGrey,
                        )
                    }

                    NovaToggle(
                        checked = quantSetting,
                        onCheckedChange = { viewModel.toggleQuantization() },
                    )
                }
            }
        }

        // Installed Models Card
        NovaCard(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = NovaSurfaceLowest,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Runtime models",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = NovaPureBlack,
                    )
                    Text(
                        text = "${runtimeModels.size} files",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = NovaAccentBlue,
                    )
                }

                runtimeModels.forEach { model ->
                    RuntimeModelRow(
                        model = model,
                        onExpandContent = onExpandContent,
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun RuntimeModelRow(
    model: RuntimeModelSpec,
    onExpandContent: (ContentDetailState) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .border(width = 1.dp, color = MaterialTheme.colorScheme.outlineVariant, shape = RoundedCornerShape(16.dp))
                .clickable {
                    onExpandContent(
                        ContentDetailState(
                            fileName = "${model.name}: ${model.version}",
                            text = "Model details:\nName: ${model.name}\nSize: ${model.version}\nStatus: ${model.status}",
                        ),
                    )
                }
                .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector =
                    when (model.iconName) {
                        "psychology" -> Icons.Filled.Psychology
                        "visibility" -> Icons.Filled.Visibility
                        "mic" -> Icons.Filled.Mic
                        else -> Icons.Filled.Storage
                    },
                contentDescription = "Model",
                tint = NovaAccentBlue,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = model.name,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                    color = NovaPureBlack,
                )
                Text(
                    text = model.version,
                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                    color = NovaSecondaryGrey,
                )
            }
        }

        NovaStatusChip(
            status = model.status,
            isPrimary = model.status == "AVAILABLE",
        )
    }
}
