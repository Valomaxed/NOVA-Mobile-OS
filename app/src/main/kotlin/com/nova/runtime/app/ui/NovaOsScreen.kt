package com.nova.runtime.app.ui

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nova.runtime.ai.model.ModelDownloadPhase
import com.nova.runtime.app.ui.components.ContentDetailSheet
import com.nova.runtime.app.ui.components.ModelDownloadOverlay
import com.nova.runtime.app.ui.components.NovaBottomNavBar
import com.nova.runtime.app.ui.components.NovaScreenTab
import com.nova.runtime.app.ui.components.NovaTopTabBar
import com.nova.runtime.app.ui.onboarding.ProfileOnboardingScreen
import com.nova.runtime.app.ui.onboarding.ProfileOnboardingViewModel
import com.nova.runtime.app.ui.tabs.ActionsTab
import com.nova.runtime.app.ui.tabs.CommandTab
import com.nova.runtime.app.ui.tabs.DashboardTab
import com.nova.runtime.app.ui.tabs.DocumentsTab
import com.nova.runtime.app.ui.tabs.SettingsTab
import com.nova.runtime.app.ui.theme.NovaColors
import com.nova.runtime.app.voice.VoiceCaptureController
import org.koin.androidx.compose.koinViewModel

/**
 * Main container with measured navigation space, safe areas, and lifecycle-aware state.
 */
@Composable
fun NovaOsScreen(
    viewModel: NovaOsViewModel,
    modifier: Modifier = Modifier,
    onboardingViewModel: ProfileOnboardingViewModel = koinViewModel(),
) {
    val lifecycleState by viewModel.lifecycleState.collectAsStateWithLifecycle()
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val activityFeed by viewModel.activityFeed.collectAsStateWithLifecycle()
    val isProcessing by viewModel.isProcessing.collectAsStateWithLifecycle()
    val isRecordingVoice by viewModel.isRecordingVoice.collectAsStateWithLifecycle()
    val whisperAvailable by viewModel.whisperAvailable.collectAsStateWithLifecycle()
    val modelDownloadState by viewModel.modelDownloadState.collectAsStateWithLifecycle()
    val indexingStatus by viewModel.indexingStatus.collectAsStateWithLifecycle()
    val contentDetail by viewModel.contentDetail.collectAsStateWithLifecycle()
    val telemetryState by viewModel.telemetryState.collectAsStateWithLifecycle()
    val showOnboarding by onboardingViewModel.showOnboarding.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val voiceCapture = remember(scope, viewModel) {
        VoiceCaptureController(context, scope, viewModel)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            voiceCapture.onPermissionGranted(whisperAvailable)
        } else {
            viewModel.reportVoiceError("Microphone permission denied.")
        }
    }

    fun toggleVoiceRecording() {
        voiceCapture.toggle(
            whisperAvailable = whisperAvailable,
            isProcessing = isProcessing,
            isRecordingVoice = isRecordingVoice,
            hasRecordAudioPermission = VoiceCaptureController.hasRecordAudioPermission(context),
            requestRecordAudioPermission = { permissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(NovaColors.background),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top Navigation Bar
            NovaTopTabBar(
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab,
                onProfileClick = onboardingViewModel::openProfileOnboarding,
                modifier = Modifier.widthIn(max = 840.dp).padding(horizontal = 20.dp),
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Model Download Overlay (if active)
            ModelDownloadOverlay(
                session = modelDownloadState,
                onRetry = viewModel::retryModelDownloads,
            )

            if (modelDownloadState.showOverlay ||
                modelDownloadState.phase == ModelDownloadPhase.FAILED ||
                modelDownloadState.phase == ModelDownloadPhase.OFFLINE
            ) {
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Main Tab Screen Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 840.dp)
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
            ) {
                run {
                    when (selectedTab) {
                        NovaScreenTab.DASHBOARD -> DashboardTab(
                            viewModel = viewModel,
                            lifecycleState = lifecycleState,
                            indexingStatus = indexingStatus,
                            activityFeed = activityFeed,
                            telemetryState = telemetryState,
                            onExpandContent = viewModel::showContentDetail,
                        )
                        NovaScreenTab.DOCUMENTS -> DocumentsTab(
                            viewModel = viewModel,
                            indexingStatus = indexingStatus,
                            telemetryState = telemetryState,
                            onExpandContent = viewModel::showContentDetail,
                        )
                        NovaScreenTab.ACTIONS -> ActionsTab(
                            viewModel = viewModel,
                            telemetryState = telemetryState,
                            onExpandContent = viewModel::showContentDetail,
                        )
                        NovaScreenTab.COMMAND -> CommandTab(
                            viewModel = viewModel,
                            onMicToggle = ::toggleVoiceRecording,
                            onExpandContent = viewModel::showContentDetail,
                        )
                        NovaScreenTab.SETTINGS -> SettingsTab(
                            viewModel = viewModel,
                            telemetryState = telemetryState,
                            onExpandContent = viewModel::showContentDetail,
                        )
                    }
                }
            }
            NovaBottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = viewModel::selectTab,
            )
        }

        // Content Detail Bottom Sheet Modal
        ContentDetailSheet(
            state = contentDetail,
            onDismiss = viewModel::dismissContentDetail,
        )

        if (showOnboarding) {
            ProfileOnboardingScreen(viewModel = onboardingViewModel)
        }
    }
}
