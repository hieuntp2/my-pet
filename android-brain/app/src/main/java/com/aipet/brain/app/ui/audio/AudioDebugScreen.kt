package com.aipet.brain.app.ui.audio

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aipet.brain.app.audio.AudioRuntimeDebugState
import com.aipet.brain.app.audio.AudioRuntimeDebugStateProvider
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.app.ui.audio.model.AudioCaptureLifecycleState
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.app.ui.audio.model.AudioDebugState
import com.aipet.brain.app.ui.audio.model.AudioCaptureRuntimeDebugState
import com.aipet.brain.app.ui.audio.model.AudioReadinessState
import com.aipet.brain.app.ui.audio.model.MicrophonePermissionState
import com.aipet.brain.perception.audio.AudioCaptureController
import com.aipet.brain.perception.audio.AudioCaptureLifecycleListener
import com.aipet.brain.perception.audio.AudioEnergyMetricsListener
import com.aipet.brain.perception.audio.AudioVadResultListener
import com.aipet.brain.perception.audio.model.VadState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun AudioDebugScreen(
    hasRequestedPermission: Boolean,
    onPermissionRequestTracked: () -> Unit,
    onNavigateBack: () -> Unit,
    audioEventBus: EventBus? = null,
    audioCaptureLifecycleListener: AudioCaptureLifecycleListener? = null,
    audioEnergyMetricsListener: AudioEnergyMetricsListener? = null,
    audioVadResultListener: AudioVadResultListener? = null,
    audioRuntimeDebugStateProvider: AudioRuntimeDebugStateProvider? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val audioCaptureController = remember(
        audioCaptureLifecycleListener,
        audioEnergyMetricsListener,
        audioVadResultListener
    ) {
        AudioCaptureController(
            lifecycleListener = audioCaptureLifecycleListener,
            energyMetricsListener = audioEnergyMetricsListener,
            vadResultListener = audioVadResultListener
        )
    }
    val playbackEngine = remember(context.applicationContext, audioEventBus) {
        AudioPlaybackEngine(
            context = context.applicationContext,
            eventBus = audioEventBus
        )
    }
    var latestEnergyMetrics by remember {
        mutableStateOf(audioCaptureController.latestEnergyMetrics())
    }
    var lastEnergyUpdatedAtMs by remember {
        mutableStateOf<Long?>(null)
    }
    var lastPlaybackRequestSummary by remember {
        mutableStateOf("No manual category playback request yet.")
    }
    val fallbackRuntimeDebugState = remember { AudioRuntimeDebugState() }
    val runtimeDebugStateFlow = audioRuntimeDebugStateProvider?.observeRuntimeDebugState()
    val initialRuntimeDebugState = audioRuntimeDebugStateProvider?.currentRuntimeDebugState()
        ?: fallbackRuntimeDebugState
    val runtimeDebugState = runtimeDebugStateFlow?.collectAsState(
        initial = initialRuntimeDebugState
    )?.value ?: fallbackRuntimeDebugState
    val playbackDebugState by playbackEngine.observeDebugState().collectAsState(
        initial = playbackEngine.currentDebugState()
    )
    var uiState by remember {
        mutableStateOf(
            createInitialAudioDebugState(
                permissionState = resolveMicrophonePermissionState(context, hasRequestedPermission),
                captureRuntimeState = readCaptureRuntimeDebugState(audioCaptureController)
            )
        )
    }

    fun withCaptureRuntime(state: AudioDebugState): AudioDebugState {
        return state.copy(captureRuntimeState = readCaptureRuntimeDebugState(audioCaptureController))
    }

    fun updateState(nextState: AudioDebugState, reason: String) {
        val enrichedState = withCaptureRuntime(nextState)
        val previousState = uiState
        logStateTransition(
            reason = reason,
            previous = previousState,
            next = enrichedState
        )
        uiState = enrichedState
    }

    fun playRandomClip(category: AudioCategory) {
        val playbackResult = playbackEngine.playRandomClipWithDetails(category)
        lastPlaybackRequestSummary = buildPlaybackRequestSummary(playbackResult)
        Log.d(TAG, "Manual playback request result: $lastPlaybackRequestSummary")
    }

    LaunchedEffect(context, hasRequestedPermission) {
        if (uiState.permissionState != MicrophonePermissionState.Requesting) {
            val permissionState = resolveMicrophonePermissionState(context, hasRequestedPermission)
            val readinessState = when {
                permissionState !is MicrophonePermissionState.Granted -> {
                    val releaseResult = audioCaptureController.release()
                    if (!releaseResult.success) {
                        Log.e(TAG, releaseResult.message)
                    }
                    AudioReadinessState.NotReadyPermissionRequired
                }
                audioCaptureController.isInitialized() -> AudioReadinessState.ReadyInitialized
                uiState.readinessState == AudioReadinessState.NotReadyFailed -> AudioReadinessState.NotReadyFailed
                else -> AudioReadinessState.ReadyToInitialize
            }
            updateState(
                nextState = uiState.copy(
                    permissionState = permissionState,
                    readinessState = readinessState,
                    captureState = if (permissionState is MicrophonePermissionState.Granted) {
                        uiState.captureState
                    } else {
                        AudioCaptureLifecycleState.Idle
                    },
                    lastErrorMessage = if (permissionState is MicrophonePermissionState.Granted) {
                        uiState.lastErrorMessage
                    } else {
                        "Microphone permission is not granted."
                    }
                ),
                reason = "permission_refresh"
            )
            if (permissionState !is MicrophonePermissionState.Granted) {
                latestEnergyMetrics = null
                lastEnergyUpdatedAtMs = null
            }
        }
    }

    LaunchedEffect(uiState.captureState) {
        if (uiState.captureState != AudioCaptureLifecycleState.Active) {
            if (!audioCaptureController.isCapturing()) {
                latestEnergyMetrics = null
                lastEnergyUpdatedAtMs = null
            }
            return@LaunchedEffect
        }

        while (audioCaptureController.isCapturing()) {
            val latestRuntimeState = readCaptureRuntimeDebugState(audioCaptureController)
            if (latestRuntimeState != uiState.captureRuntimeState) {
                updateState(
                    nextState = uiState.copy(captureRuntimeState = latestRuntimeState),
                    reason = "capture_runtime_poll"
                )
            }
            delay(RUNTIME_POLL_INTERVAL_MS)
        }

        val idleRuntimeState = readCaptureRuntimeDebugState(audioCaptureController)
        if (idleRuntimeState != uiState.captureRuntimeState) {
            updateState(
                nextState = uiState.copy(captureRuntimeState = idleRuntimeState),
                reason = "capture_runtime_idle_refresh"
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        val permissionState = resolveMicrophonePermissionState(context, hasRequestedPermission = true)
        val releaseResult = if (permissionState is MicrophonePermissionState.Granted) {
            null
        } else {
            audioCaptureController.release()
        }
        val lastErrorMessage = when {
            permissionState is MicrophonePermissionState.Granted -> uiState.lastErrorMessage
            releaseResult != null && !releaseResult.success -> releaseResult.message
            else -> "Microphone permission is not granted."
        }
        updateState(
            nextState = uiState.copy(
                permissionState = permissionState,
                readinessState = when {
                    permissionState !is MicrophonePermissionState.Granted -> {
                        AudioReadinessState.NotReadyPermissionRequired
                    }
                    audioCaptureController.isInitialized() -> AudioReadinessState.ReadyInitialized
                    else -> AudioReadinessState.ReadyToInitialize
                },
                captureState = if (permissionState is MicrophonePermissionState.Granted) {
                    uiState.captureState
                } else {
                    AudioCaptureLifecycleState.Idle
                },
                lastErrorMessage = lastErrorMessage
            ),
            reason = "permission_result"
        )
        if (permissionState !is MicrophonePermissionState.Granted) {
            latestEnergyMetrics = null
            lastEnergyUpdatedAtMs = null
        }
    }

    val requestPermission = {
        onPermissionRequestTracked()
        updateState(
            nextState = uiState.copy(
                permissionState = MicrophonePermissionState.Requesting,
                readinessState = AudioReadinessState.NotReadyPermissionRequired
            ),
            reason = "permission_request_started"
        )
        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val initializeAudio = initializeAudio@{
        if (uiState.permissionState !is MicrophonePermissionState.Granted) {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.NotReadyPermissionRequired,
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = "Cannot initialize audio without microphone permission."
                ),
                reason = "initialize_without_permission"
            )
            return@initializeAudio
        }

        val initializeResult = audioCaptureController.initialize()
        if (initializeResult.success) {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.ReadyInitialized,
                    captureState = AudioCaptureLifecycleState.Idle,
                    lastErrorMessage = null
                ),
                reason = "initialize_success"
            )
        } else {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.NotReadyFailed,
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = initializeResult.message
                ),
                reason = "initialize_failed"
            )
        }
    }

    val startCapture = startCapture@{
        if (uiState.permissionState !is MicrophonePermissionState.Granted) {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.NotReadyPermissionRequired,
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = "Cannot start capture without microphone permission."
                ),
                reason = "start_without_permission"
            )
            return@startCapture
        }
        if (!audioCaptureController.isInitialized()) {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.ReadyToInitialize,
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = "Initialize audio before starting capture."
                ),
                reason = "start_without_initialization"
            )
            return@startCapture
        }

        updateState(
            nextState = uiState.copy(captureState = AudioCaptureLifecycleState.Starting),
            reason = "start_requested"
        )
        latestEnergyMetrics = null
        lastEnergyUpdatedAtMs = null
        val startResult = audioCaptureController.startCapture()
        if (startResult.success) {
            updateState(
                nextState = uiState.copy(
                    readinessState = AudioReadinessState.ReadyInitialized,
                    captureState = AudioCaptureLifecycleState.Active,
                    lastErrorMessage = null
                ),
                reason = "start_success"
            )
        } else {
            updateState(
                nextState = uiState.copy(
                    readinessState = if (audioCaptureController.isInitialized()) {
                        AudioReadinessState.ReadyInitialized
                    } else {
                        AudioReadinessState.ReadyToInitialize
                    },
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = startResult.message
                ),
                reason = "start_failed"
            )
        }
    }

    val stopCapture = {
        val stopResult = audioCaptureController.stopCapture()
        if (stopResult.success) {
            updateState(
                nextState = uiState.copy(
                    readinessState = if (uiState.permissionState is MicrophonePermissionState.Granted) {
                        AudioReadinessState.ReadyInitialized
                    } else {
                        AudioReadinessState.NotReadyPermissionRequired
                    },
                    captureState = AudioCaptureLifecycleState.Stopped,
                    lastErrorMessage = null
                ),
                reason = "stop_success"
            )
            latestEnergyMetrics = null
            lastEnergyUpdatedAtMs = null
        } else {
            updateState(
                nextState = uiState.copy(
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = stopResult.message
                ),
                reason = "stop_failed"
            )
        }
    }

    val releaseAudio = {
        val releaseResult = audioCaptureController.release()
        if (releaseResult.success) {
            updateState(
                nextState = uiState.copy(
                    readinessState = if (uiState.permissionState is MicrophonePermissionState.Granted) {
                        AudioReadinessState.ReadyToInitialize
                    } else {
                        AudioReadinessState.NotReadyPermissionRequired
                    },
                    captureState = AudioCaptureLifecycleState.Idle,
                    lastErrorMessage = null
                ),
                reason = "release_success"
            )
            latestEnergyMetrics = null
            lastEnergyUpdatedAtMs = null
        } else {
            updateState(
                nextState = uiState.copy(
                    readinessState = if (uiState.permissionState is MicrophonePermissionState.Granted) {
                        AudioReadinessState.NotReadyFailed
                    } else {
                        AudioReadinessState.NotReadyPermissionRequired
                    },
                    captureState = AudioCaptureLifecycleState.Failed,
                    lastErrorMessage = releaseResult.message
                ),
                reason = "release_failed"
            )
        }
    }

    DisposableEffect(lifecycleOwner, context, hasRequestedPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (
                event == Lifecycle.Event.ON_RESUME &&
                uiState.permissionState != MicrophonePermissionState.Requesting
            ) {
                val permissionState = resolveMicrophonePermissionState(context, hasRequestedPermission)
                val releaseResult = if (permissionState is MicrophonePermissionState.Granted) {
                    null
                } else {
                    audioCaptureController.release()
                }
                updateState(
                    nextState = uiState.copy(
                        permissionState = permissionState,
                        readinessState = when {
                            permissionState !is MicrophonePermissionState.Granted -> {
                                AudioReadinessState.NotReadyPermissionRequired
                            }
                            audioCaptureController.isInitialized() -> AudioReadinessState.ReadyInitialized
                            uiState.readinessState == AudioReadinessState.NotReadyFailed -> {
                                AudioReadinessState.NotReadyFailed
                            }
                            else -> AudioReadinessState.ReadyToInitialize
                        },
                        captureState = if (permissionState is MicrophonePermissionState.Granted) {
                            uiState.captureState
                        } else {
                            AudioCaptureLifecycleState.Idle
                        },
                        lastErrorMessage = when {
                            permissionState is MicrophonePermissionState.Granted -> uiState.lastErrorMessage
                            releaseResult != null && !releaseResult.success -> releaseResult.message
                            else -> "Microphone permission is not granted."
                        }
                    ),
                    reason = "resume_refresh"
                )
                if (permissionState !is MicrophonePermissionState.Granted) {
                    latestEnergyMetrics = null
                    lastEnergyUpdatedAtMs = null
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    DisposableEffect(audioCaptureController, playbackEngine, mainHandler) {
        audioCaptureController.setEnergyMetricsCallback { metrics ->
            mainHandler.post {
                latestEnergyMetrics = metrics
                lastEnergyUpdatedAtMs = System.currentTimeMillis()
            }
        }
        onDispose {
            audioCaptureController.setEnergyMetricsCallback(null)
            val releaseResult = audioCaptureController.release()
            if (!releaseResult.success) {
                Log.e(TAG, releaseResult.message)
            }
            playbackEngine.release()
        }
    }

    val canInitialize = uiState.permissionState is MicrophonePermissionState.Granted &&
        !audioCaptureController.isInitialized()
    val canStart = uiState.permissionState is MicrophonePermissionState.Granted &&
        audioCaptureController.isInitialized() &&
        !audioCaptureController.isCapturing()
    val canStop = audioCaptureController.isCapturing()
    val canRelease = audioCaptureController.isInitialized()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Audio Debug")
        Text(
            text = "Audio capture, VAD-light, and sound-event debug (C102)",
            modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
        )
        Text(
            text = "Permission: ${permissionStateLabel(uiState.permissionState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Readiness: ${readinessStateLabel(uiState.readinessState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Capture: ${captureStateLabel(uiState.captureState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Audio initialized: ${if (audioCaptureController.isInitialized()) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Capture active: ${if (audioCaptureController.isCapturing()) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Capture Runtime",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "Source running: ${if (uiState.captureRuntimeState.isRunning) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Sample rate (Hz): ${formatMetricValue(uiState.captureRuntimeState.sampleRateHz)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Channel count: ${formatMetricValue(uiState.captureRuntimeState.channelCount)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Frame size (samples): ${formatMetricValue(uiState.captureRuntimeState.frameSizeSamples)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Energy Metrics",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        AudioEnergyPanel(
            isCaptureActive = audioCaptureController.isCapturing(),
            metrics = latestEnergyMetrics,
            lastUpdatedAtMs = lastEnergyUpdatedAtMs
        )
        Text(
            text = "VAD state: ${formatVadStateLabel(audioCaptureController.isCapturing(), runtimeDebugState.vadState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last sound event: ${formatSoundEventType(runtimeDebugState.lastSoundEventType)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last sound event seq: ${formatSoundEventSequence(runtimeDebugState.lastSoundEventSequence)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last sound event time: ${formatSoundEventTimestamp(runtimeDebugState.lastSoundEventTimestampMs)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Note: VOICE_LIKELY is energy-based (not speech recognition).",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last error: ${uiState.lastErrorMessage ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        AudioAssetManifestSection(context = context)

        when (val state = uiState.permissionState) {
            MicrophonePermissionState.NotRequested -> {
                Text(
                    text = "Microphone permission has not been requested yet.",
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = requestPermission,
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .fillMaxWidth()
                ) {
                    Text(text = "Request Microphone Permission")
                }
            }

            MicrophonePermissionState.Requesting -> {
                Text(
                    text = "Waiting for system permission result...",
                    modifier = Modifier.fillMaxWidth()
                )
            }

            is MicrophonePermissionState.Denied -> {
                if (state.canRequestAgain) {
                    Text(
                        text = "Microphone permission was denied. You can request it again.",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = requestPermission,
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = "Request Again")
                    }
                } else {
                    Text(
                        text = "Microphone permission is blocked. Enable it in app settings.",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = { openAppSettings(context) },
                        modifier = Modifier
                            .padding(top = 12.dp, bottom = 8.dp)
                            .fillMaxWidth()
                    ) {
                        Text(text = "Open App Settings")
                    }
                }
            }

            MicrophonePermissionState.Granted -> {
                Text(
                    text = "Microphone permission granted.",
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = initializeAudio,
            enabled = canInitialize,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Initialize Audio")
        }

        Button(
            onClick = startCapture,
            enabled = canStart,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Start Capture")
        }

        Button(
            onClick = stopCapture,
            enabled = canStop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Stop Capture")
        }

        Button(
            onClick = releaseAudio,
            enabled = canRelease,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Release Audio")
        }

        Text(
            text = "Test Sounds",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "Playback readiness: ${formatPlaybackReadiness(playbackDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Preload clips: ${playbackDebugState.loadedClipCount}/" +
                "${playbackDebugState.totalClipCount} (failed=${playbackDebugState.failedClipCount})",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last played clip: ${formatLastPlayedClip(playbackDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last skipped reason: ${formatLastSkippedReason(playbackDebugState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last manual playback request: $lastPlaybackRequestSummary",
            modifier = Modifier.fillMaxWidth()
        )

        AudioCategory.entries.forEach { category ->
            Button(
                onClick = { playRandomClip(category) },
                enabled = AudioAssetRegistry.hasClips(category),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Play ${category.label}")
            }
        }

        Button(
            onClick = onNavigateBack,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Back to Debug")
        }
    }
}

@Composable
private fun AudioAssetManifestSection(context: Context) {
    Text(
        text = "Audio Asset Manifest",
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
    )
    Text(
        text = "Runtime source: flattened R.raw.* entries from AudioAssetRegistry.",
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Nested duplicate folders under res/raw/<category>/ are ignored.",
        modifier = Modifier.fillMaxWidth()
    )

    AudioCategory.entries.forEach { category ->
        val clipMetadataList = AudioAssetRegistry.getClipMetadata(category)
        Text(
            text = "${category.label}: ${clipMetadataList.size} clip(s)",
            modifier = Modifier.fillMaxWidth()
        )
        if (clipMetadataList.isEmpty()) {
            Text(
                text = "No clips configured.",
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 12.dp)
            )
        } else {
            clipMetadataList.forEach { clipMetadata ->
                val rawResourceName = resolveRawResourceName(
                    context = context,
                    resourceId = clipMetadata.resourceId
                )
                val durationLabel = clipMetadata.durationMs?.let { "$it ms" } ?: "unknown"
                Text(
                    text = "${clipMetadata.logicalClipName} | R.raw.$rawResourceName | duration=$durationLabel",
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp)
                )
            }
        }
    }
}

private fun resolveRawResourceName(
    context: Context,
    resourceId: Int
): String {
    return try {
        context.resources.getResourceEntryName(resourceId)
    } catch (_: Resources.NotFoundException) {
        "unresolved_$resourceId"
    }
}

private fun resolveMicrophonePermissionState(
    context: Context,
    hasRequestedPermission: Boolean
): MicrophonePermissionState {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        return MicrophonePermissionState.Granted
    }
    if (!hasRequestedPermission) {
        return MicrophonePermissionState.NotRequested
    }

    val activity = context.findActivity()
    val canRequestAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.RECORD_AUDIO)
    } ?: false
    return MicrophonePermissionState.Denied(canRequestAgain = canRequestAgain)
}

private fun openAppSettings(context: Context) {
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(detailsIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            // No settings activity is available on this device.
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun createInitialAudioDebugState(
    permissionState: MicrophonePermissionState,
    captureRuntimeState: AudioCaptureRuntimeDebugState
): AudioDebugState {
    return AudioDebugState(
        permissionState = permissionState,
        readinessState = if (permissionState is MicrophonePermissionState.Granted) {
            AudioReadinessState.ReadyToInitialize
        } else {
            AudioReadinessState.NotReadyPermissionRequired
        },
        captureState = AudioCaptureLifecycleState.Idle,
        lastErrorMessage = if (permissionState is MicrophonePermissionState.Granted) {
            null
        } else {
            "Microphone permission is not granted."
        },
        captureRuntimeState = captureRuntimeState
    )
}

private fun permissionStateLabel(state: MicrophonePermissionState): String {
    return when (state) {
        MicrophonePermissionState.NotRequested -> "Not requested"
        MicrophonePermissionState.Requesting -> "Requesting"
        is MicrophonePermissionState.Denied -> {
            if (state.canRequestAgain) {
                "Denied (can request again)"
            } else {
                "Denied (open app settings)"
            }
        }
        MicrophonePermissionState.Granted -> "Granted"
    }
}

private fun readinessStateLabel(state: AudioReadinessState): String {
    return when (state) {
        AudioReadinessState.NotReadyPermissionRequired -> "Not ready (permission required)"
        AudioReadinessState.ReadyToInitialize -> "Ready to initialize"
        AudioReadinessState.ReadyInitialized -> "Ready (initialized)"
        AudioReadinessState.NotReadyFailed -> "Not ready (failed)"
    }
}

private fun captureStateLabel(state: AudioCaptureLifecycleState): String {
    return when (state) {
        AudioCaptureLifecycleState.Idle -> "Idle"
        AudioCaptureLifecycleState.Starting -> "Starting"
        AudioCaptureLifecycleState.Active -> "Active"
        AudioCaptureLifecycleState.Stopped -> "Stopped"
        AudioCaptureLifecycleState.Failed -> "Failed"
    }
}

private fun logStateTransition(
    reason: String,
    previous: AudioDebugState,
    next: AudioDebugState
) {
    if (previous == next) {
        Log.d(TAG, "State unchanged ($reason)")
        return
    }

    Log.d(
        TAG,
        "State transition ($reason): " +
            "permission=${permissionStateLabel(previous.permissionState)} -> ${permissionStateLabel(next.permissionState)}, " +
            "readiness=${readinessStateLabel(previous.readinessState)} -> ${readinessStateLabel(next.readinessState)}, " +
            "capture=${captureStateLabel(previous.captureState)} -> ${captureStateLabel(next.captureState)}, " +
            "runtimeRunning=${if (previous.captureRuntimeState.isRunning) "Yes" else "No"} -> " +
            "${if (next.captureRuntimeState.isRunning) "Yes" else "No"}, " +
            "lastError=${next.lastErrorMessage ?: "-"}"
    )
}

private fun readCaptureRuntimeDebugState(
    audioCaptureController: AudioCaptureController
): AudioCaptureRuntimeDebugState {
    val runtimeState = audioCaptureController.captureRuntimeState()
    return AudioCaptureRuntimeDebugState(
        isRunning = runtimeState.isRunning,
        sampleRateHz = runtimeState.sampleRate.takeIf { it > 0 },
        channelCount = runtimeState.channelCount.takeIf { it > 0 },
        frameSizeSamples = runtimeState.frameSize.takeIf { it > 0 }
    )
}

private fun formatMetricValue(value: Int?): String {
    return value?.toString() ?: "-"
}

private fun formatVadStateLabel(
    isCaptureActive: Boolean,
    vadState: VadState?
): String {
    if (!isCaptureActive) {
        return "IDLE"
    }
    return vadState?.name ?: VadState.SILENT.name
}

private fun formatSoundEventType(eventType: EventType?): String {
    return eventType?.name ?: "-"
}

private fun formatSoundEventSequence(sequence: Long): String {
    return if (sequence > 0L) {
        sequence.toString()
    } else {
        "-"
    }
}

private fun formatSoundEventTimestamp(timestampMs: Long?): String {
    val timestamp = timestampMs ?: return "-"
    val formatter = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    return "${formatter.format(Date(timestamp))} ($timestamp)"
}

private fun buildPlaybackRequestSummary(result: AudioPlaybackResult): String {
    val categoryLabel = result.category.label
    val clipLabel = result.clipLogicalName ?: "-"
    val resourceLabel = result.clipResourceName?.let { "R.raw.$it" } ?: "-"
    val clipIdLabel = result.clipResId?.toString() ?: "-"
    val statusLabel = if (result.started) "STARTED" else "SKIPPED"
    return "category=$categoryLabel, clip=$clipLabel, resource=$resourceLabel, " +
        "clipResId=$clipIdLabel, status=$statusLabel, reason=${result.reason}"
}

private fun formatPlaybackReadiness(state: AudioPlaybackDebugState): String {
    return state.readinessState.name
}

private fun formatLastPlayedClip(state: AudioPlaybackDebugState): String {
    val category = state.lastPlayedCategory ?: return "-"
    val clip = state.lastPlayedClipName ?: "-"
    val timestamp = state.lastPlayedAtMs?.let(::formatSoundEventTimestamp) ?: "-"
    return "$category / $clip @ $timestamp"
}

private fun formatLastSkippedReason(state: AudioPlaybackDebugState): String {
    val reason = state.lastSkippedReason?.name ?: return "-"
    val timestamp = state.lastSkippedAtMs?.let(::formatSoundEventTimestamp) ?: "-"
    return "$reason @ $timestamp"
}

private const val TAG = "AudioDebugScreen"
private const val RUNTIME_POLL_INTERVAL_MS = 300L

