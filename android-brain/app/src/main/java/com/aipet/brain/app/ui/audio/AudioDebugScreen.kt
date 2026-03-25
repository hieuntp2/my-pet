package com.aipet.brain.app.ui.audio

import android.Manifest
import android.content.Context
import android.content.res.Resources
import android.os.Handler
import android.os.Looper
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aipet.brain.app.permissions.openAppSettings
import com.aipet.brain.app.permissions.resolveMicrophonePermissionState
import com.aipet.brain.app.audio.AudioRuntimeDebugState
import com.aipet.brain.app.audio.AudioRuntimeDebugStateProvider
import com.aipet.brain.app.settings.KeywordSpottingConfigStore
import com.aipet.brain.app.ui.debug.DebugBackButton
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioResponsePayload
import com.aipet.brain.brain.events.audio.AudioResponseRequestPayload
import com.aipet.brain.brain.events.audio.KeywordDetectionPayload
import com.aipet.brain.app.ui.audio.model.AudioCaptureLifecycleState
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.app.ui.audio.model.AudioDebugState
import com.aipet.brain.app.ui.audio.model.AudioCaptureRuntimeDebugState
import com.aipet.brain.app.ui.audio.model.AudioReadinessState
import com.aipet.brain.app.ui.audio.model.MicrophonePermissionState
import com.aipet.brain.core.common.config.KeywordSpottingConfig
import com.aipet.brain.core.common.config.KeywordSpottingProvider
import com.aipet.brain.perception.audio.AudioCaptureController
import com.aipet.brain.perception.audio.AudioCaptureLifecycleListener
import com.aipet.brain.perception.audio.AudioEnergyMetricsListener
import com.aipet.brain.perception.audio.AudioKeywordDetectionListener
import com.aipet.brain.perception.audio.AudioVadResultListener
import com.aipet.brain.perception.audio.keyword.KeywordSpotterFactory
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordSpotterState
import com.aipet.brain.perception.audio.model.VadState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@Composable
fun AudioDebugScreen(
    hasRequestedPermission: Boolean,
    onPermissionRequestTracked: () -> Unit,
    onNavigateBack: () -> Unit,
    audioEventBus: EventBus? = null,
    audioPlaybackEngine: AudioPlaybackEngine? = null,
    audioCaptureLifecycleListener: AudioCaptureLifecycleListener? = null,
    audioEnergyMetricsListener: AudioEnergyMetricsListener? = null,
    audioVadResultListener: AudioVadResultListener? = null,
    audioKeywordDetectionListener: AudioKeywordDetectionListener? = null,
    audioRuntimeDebugStateProvider: AudioRuntimeDebugStateProvider? = null,
    keywordSpottingConfigStore: KeywordSpottingConfigStore
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val audioCaptureController = remember(
        audioCaptureLifecycleListener,
        audioEnergyMetricsListener,
        audioVadResultListener,
        audioKeywordDetectionListener
    ) {
        AudioCaptureController(
            lifecycleListener = audioCaptureLifecycleListener,
            energyMetricsListener = audioEnergyMetricsListener,
            vadResultListener = audioVadResultListener,
            keywordDetectionListener = audioKeywordDetectionListener
        )
    }
    val ownedPlaybackEngine = remember(context.applicationContext, audioEventBus, audioPlaybackEngine) {
        if (audioPlaybackEngine == null) {
            AudioPlaybackEngine(
                context = context.applicationContext,
                eventBus = audioEventBus
            )
        } else {
            null
        }
    }
    val playbackEngine = audioPlaybackEngine ?: ownedPlaybackEngine
        ?: error("AudioPlaybackEngine must be available.")
    val coroutineScope = rememberCoroutineScope()
    val keywordSpottingConfig by keywordSpottingConfigStore.config.collectAsState(
        initial = KeywordSpottingConfig.DEFAULT
    )
    val keywordSpotterFactory = remember { KeywordSpotterFactory() }
    var keywordProviderRuntimeNote by remember { mutableStateOf<String?>(null) }
    var keywordSpotterRuntimeState by remember { mutableStateOf(KeywordSpotterState.IDLE) }
    var keywordDetectionCount by remember { mutableStateOf(0L) }
    var lastKeywordDetection by remember { mutableStateOf<KeywordDetectionResult?>(null) }
    var keywordSpotterLastError by remember { mutableStateOf<String?>(null) }
    var latestEnergyMetrics by remember {
        mutableStateOf(audioCaptureController.latestEnergyMetrics())
    }
    var lastEnergyUpdatedAtMs by remember {
        mutableStateOf<Long?>(null)
    }
    var lastPlaybackRequestSummary by remember {
        mutableStateOf("No manual EventBus playback request yet.")
    }
    var smokeTestState by remember {
        mutableStateOf(AudioMvpSmokeTestState())
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

    fun refreshKeywordRuntimeState() {
        keywordSpotterRuntimeState = audioCaptureController.keywordSpotterState()
        keywordDetectionCount = audioCaptureController.keywordDetectionCount()
        lastKeywordDetection = audioCaptureController.latestKeywordDetectionResult()
        keywordSpotterLastError = audioCaptureController.keywordSpotterLastErrorMessage()
    }

    fun playRandomClip(category: AudioCategory) {
        val eventBus = audioEventBus
        if (eventBus == null) {
            val playbackResult = playbackEngine.playRandomClipWithDetails(category)
            lastPlaybackRequestSummary = buildPlaybackRequestSummary(playbackResult)
            Log.d(TAG, "Manual playback request result (direct fallback): $lastPlaybackRequestSummary")
            return
        }

        val requestedAtMs = System.currentTimeMillis()
        val requestPayload = AudioResponseRequestPayload(
            category = category.label,
            priority = 1,
            interruptPolicy = "INTERRUPT_NONE",
            cooldownKey = category.label,
            timestamp = requestedAtMs
        )
        coroutineScope.launch {
            try {
                eventBus.publish(
                    EventEnvelope.create(
                        type = EventType.AUDIO_RESPONSE_REQUESTED,
                        timestampMs = requestedAtMs,
                        payloadJson = requestPayload.toJson()
                    )
                )
                lastPlaybackRequestSummary =
                    "event=${EventType.AUDIO_RESPONSE_REQUESTED.name}, category=${category.label}, " +
                        "timestamp=$requestedAtMs"
                Log.d(TAG, "Published manual ${EventType.AUDIO_RESPONSE_REQUESTED.name}: $lastPlaybackRequestSummary")
            } catch (error: Throwable) {
                lastPlaybackRequestSummary =
                    "event=${EventType.AUDIO_RESPONSE_REQUESTED.name}, category=${category.label}, " +
                        "status=FAILED, error=${error.message ?: "unknown"}"
                Log.e(
                    TAG,
                    "Failed to publish manual ${EventType.AUDIO_RESPONSE_REQUESTED.name}. " +
                        "category=${category.label}",
                    error
                )
            }
        }
    }

    fun setKeywordSpottingEnabled(enabled: Boolean) {
        coroutineScope.launch {
            try {
                keywordSpottingConfigStore.setEnabled(enabled)
                Log.d(TAG, "Keyword spotting enabled set to $enabled")
            } catch (error: Throwable) {
                val message = "Failed to update keyword spotting enabled state."
                Log.e(TAG, message, error)
                updateState(
                    nextState = uiState.copy(lastErrorMessage = "$message ${error.message ?: "unknown error"}"),
                    reason = "keyword_config_enabled_failed"
                )
            }
        }
    }

    fun setKeywordSpottingProvider(provider: KeywordSpottingProvider) {
        coroutineScope.launch {
            try {
                keywordSpottingConfigStore.setProvider(provider)
                Log.d(TAG, "Keyword spotting provider set to ${provider.name}")
            } catch (error: Throwable) {
                val message = "Failed to update keyword spotting provider."
                Log.e(TAG, message, error)
                updateState(
                    nextState = uiState.copy(lastErrorMessage = "$message ${error.message ?: "unknown error"}"),
                    reason = "keyword_config_provider_failed"
                )
            }
        }
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

    LaunchedEffect(keywordSpottingConfig.enabled, keywordSpottingConfig.provider) {
        if (!keywordSpottingConfig.enabled) {
            audioCaptureController.setKeywordSpotter(null)
            keywordProviderRuntimeNote = "Keyword spotting is disabled in config."
            refreshKeywordRuntimeState()
            return@LaunchedEffect
        }

        val selectedProvider = keywordSpottingConfig.provider
        val selectedSpotter = keywordSpotterFactory.create(selectedProvider)
        if (selectedSpotter == null) {
            audioCaptureController.setKeywordSpotter(null)
            keywordProviderRuntimeNote =
                "Provider ${selectedProvider.displayName} is not integrated in this build."
        } else {
            audioCaptureController.setKeywordSpotter(selectedSpotter)
            keywordProviderRuntimeNote =
                "Provider ${selectedProvider.displayName} uses adapter ${selectedSpotter.spotterId}."
        }
        refreshKeywordRuntimeState()
    }

    LaunchedEffect(audioEventBus) {
        smokeTestState = AudioMvpSmokeTestState()
        val eventBus = audioEventBus ?: return@LaunchedEffect
        eventBus.observe().collect { event ->
            smokeTestState = smokeTestState.consume(event)
        }
    }

    LaunchedEffect(uiState.captureState) {
        if (uiState.captureState != AudioCaptureLifecycleState.Active) {
            if (!audioCaptureController.isCapturing()) {
                latestEnergyMetrics = null
                lastEnergyUpdatedAtMs = null
            }
            refreshKeywordRuntimeState()
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
            refreshKeywordRuntimeState()
            delay(RUNTIME_POLL_INTERVAL_MS)
        }

        val idleRuntimeState = readCaptureRuntimeDebugState(audioCaptureController)
        if (idleRuntimeState != uiState.captureRuntimeState) {
            updateState(
                nextState = uiState.copy(captureRuntimeState = idleRuntimeState),
                reason = "capture_runtime_idle_refresh"
            )
        }
        refreshKeywordRuntimeState()
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
            refreshKeywordRuntimeState()
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
        refreshKeywordRuntimeState()
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
            refreshKeywordRuntimeState()
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
            refreshKeywordRuntimeState()
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
        refreshKeywordRuntimeState()
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
        refreshKeywordRuntimeState()
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
        refreshKeywordRuntimeState()
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

    DisposableEffect(audioCaptureController, ownedPlaybackEngine, mainHandler) {
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
            ownedPlaybackEngine?.release()
        }
    }

    val canInitialize = uiState.permissionState is MicrophonePermissionState.Granted &&
        !audioCaptureController.isInitialized()
    val canStart = uiState.permissionState is MicrophonePermissionState.Granted &&
        audioCaptureController.isInitialized() &&
        !audioCaptureController.isCapturing()
    val canStop = audioCaptureController.isCapturing()
    val canRelease = audioCaptureController.isInitialized()
    val wakeWordCheckpoint = evaluateWakeWordCheckpoint(
        config = keywordSpottingConfig,
        runtimeState = keywordSpotterRuntimeState,
        runtimeNote = keywordProviderRuntimeNote,
        wakeWordObservation = smokeTestState.lastWakeWordDetection
    )
    val keywordCheckpoint = evaluateKeywordCheckpoint(
        keywordObservation = smokeTestState.lastKeywordDetection
    )

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
            text = "Keyword Spotting Config",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "Keyword spotting enabled: ${if (keywordSpottingConfig.enabled) "Yes" else "No"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Selected provider: ${keywordSpottingConfig.provider.displayName}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Runtime status: ${formatKeywordSpottingRuntimeStatus(keywordSpottingConfig, keywordSpotterRuntimeState, keywordProviderRuntimeNote)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Keyword adapter state: ${keywordSpotterRuntimeState.name}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Keyword detections: $keywordDetectionCount",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last keyword detection: ${formatKeywordDetection(lastKeywordDetection)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Keyword adapter note: ${keywordProviderRuntimeNote ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Keyword adapter error: ${keywordSpotterLastError ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Audio MVP Smoke Test (Manual)",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "1) Request permission -> Initialize Audio -> Start Capture.",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "2) Speak near mic: expect VOICE_ACTIVITY_* and behavior request.",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "3) Make a short loud sound: expect surprised request path.",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "4) If supported, trigger wake word: expect WAKE_WORD_DETECTED.",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "5) Confirm request -> playback lifecycle, then Stop Capture.",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.captureStartedAtMs != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Capture started event observed",
                detail = smokeTestState.captureStartedAtMs?.let(::formatSoundEventTimestamp)
                    ?: "waiting for ${EventType.AUDIO_CAPTURE_STARTED.name}"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.captureStoppedAtMs != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Capture stopped event observed",
                detail = smokeTestState.captureStoppedAtMs?.let(::formatSoundEventTimestamp)
                    ?: "stop capture to verify ${EventType.AUDIO_CAPTURE_STOPPED.name}"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.energyEventAtMs != null || runtimeDebugState.latestEnergyTimestampMs != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Energy/VAD activity observed",
                detail = (
                    smokeTestState.energyEventAtMs
                        ?: runtimeDebugState.latestEnergyTimestampMs
                    )?.let(::formatSoundEventTimestamp)
                    ?: "waiting for ${EventType.SOUND_ENERGY_CHANGED.name}"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.voiceActivityStartedAtMs != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Voice activity observed",
                detail = smokeTestState.voiceActivityStartedAtMs?.let(::formatSoundEventTimestamp)
                    ?: "waiting for ${EventType.VOICE_ACTIVITY_STARTED.name}"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.lastBehaviorResponseRequest != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Behavior request observed",
                detail = smokeTestState.lastBehaviorResponseRequest?.let(::formatRequestObservation)
                    ?: "waiting for request after sound/voice/wake stimulus"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = if (smokeTestState.lastPlaybackEvent != null) {
                    SmokeCheckpointStatus.PASS
                } else {
                    SmokeCheckpointStatus.PENDING
                },
                label = "Playback lifecycle observed",
                detail = smokeTestState.lastPlaybackEvent?.let(::formatPlaybackObservation)
                    ?: "waiting for ${EventType.AUDIO_RESPONSE_STARTED.name}/" +
                        "${EventType.AUDIO_RESPONSE_COMPLETED.name}/" +
                        "${EventType.AUDIO_RESPONSE_SKIPPED.name}"
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = wakeWordCheckpoint.status,
                label = "Wake-word detected",
                detail = wakeWordCheckpoint.detail
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = formatSmokeCheckpoint(
                status = keywordCheckpoint.status,
                label = "Keyword detected (optional)",
                detail = keywordCheckpoint.detail
            ),
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Latest sound event: ${formatLatestSmokeSoundEvent(runtimeDebugState, smokeTestState)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Latest request event: ${formatRequestObservation(smokeTestState.lastResponseRequest)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Latest playback outcome: ${formatPlaybackObservation(smokeTestState.lastPlaybackEvent)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Latest wake-word event: ${formatKeywordObservation(smokeTestState.lastWakeWordDetection)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Known limits: first playback may skip NOT_READY; cooldown/overlap guard may skip requests; keyword confidence still needs tuning.",
            modifier = Modifier.fillMaxWidth()
        )
        Button(
            onClick = { smokeTestState = AudioMvpSmokeTestState() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(text = "Reset Smoke Session")
        }
        Button(
            onClick = { setKeywordSpottingEnabled(!keywordSpottingConfig.enabled) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = if (keywordSpottingConfig.enabled) {
                    "Disable Keyword Spotting"
                } else {
                    "Enable Keyword Spotting"
                }
            )
        }
        KeywordSpottingProvider.entries.forEach { provider ->
            Button(
                onClick = { setKeywordSpottingProvider(provider) },
                enabled = keywordSpottingConfig.provider != provider,
                modifier = Modifier.fillMaxWidth()
            ) {
                val label = if (keywordSpottingConfig.provider == provider) {
                    "Provider: ${provider.displayName} (Selected)"
                } else {
                    "Select Provider: ${provider.displayName}"
                }
                Text(text = label)
            }
        }
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

        FullWidthLine(
            text = "Test Sounds",
            modifier = Modifier.padding(top = 8.dp)
        )
        FullWidthLine(text = "Playback readiness: ${formatPlaybackReadiness(playbackDebugState)}")
        FullWidthLine(
            text = "Preload clips: ${playbackDebugState.loadedClipCount}/" +
                "${playbackDebugState.totalClipCount} (failed=${playbackDebugState.failedClipCount})"
        )
        FullWidthLine(text = "Last played clip: ${formatLastPlayedClip(playbackDebugState)}")
        FullWidthLine(text = "Last skipped reason: ${formatLastSkippedReason(playbackDebugState)}")
        FullWidthLine(text = "Last manual playback request: $lastPlaybackRequestSummary")

        AudioCategory.entries.forEach { category ->
            ActionButton(
                label = "Play ${category.label}",
                onClick = { playRandomClip(category) },
                enabled = AudioAssetRegistry.hasClips(category),
                modifier = Modifier.fillMaxWidth()
            )
        }

        DebugBackButton(
            onClick = onNavigateBack,
            outlined = false,
            modifier = Modifier
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth()
        )
    }
}

@Composable
private fun AudioAssetManifestSection(context: Context) {
    FullWidthLine(
        text = "Audio Asset Manifest",
        modifier = Modifier.padding(top = 8.dp)
    )
    FullWidthLine(text = "Runtime source: flattened R.raw.* entries from AudioAssetRegistry.")
    FullWidthLine(text = "Nested duplicate folders under res/raw/<category>/ are ignored.")

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

@Composable
private fun FullWidthLine(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        modifier = modifier.fillMaxWidth()
    )
}

@Composable
private fun ActionButton(
    label: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = label)
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

private fun formatKeywordSpottingRuntimeStatus(
    config: KeywordSpottingConfig,
    runtimeState: KeywordSpotterState,
    runtimeNote: String?
): String {
    if (!config.enabled) {
        return "Disabled by config. No keyword runtime path is active."
    }
    return when (config.provider) {
        KeywordSpottingProvider.NONE -> "Enabled, provider=None. No adapter is active."
        else -> {
            runtimeNote ?: "Provider ${config.provider.displayName} selected, adapter unavailable. " +
                "Adapter state=${runtimeState.name}."
        }
    }
}

private fun formatKeywordDetection(result: KeywordDetectionResult?): String {
    val activeResult = result ?: return "-"
    val confidenceLabel = String.format(Locale.US, "%.3f", activeResult.confidence)
    return "${activeResult.detectionType.name} ${activeResult.keywordId} " +
        "(text=${activeResult.keywordText ?: "-"}) " +
        "confidence=$confidenceLabel engine=${activeResult.engineName} " +
        "@${formatSoundEventTimestamp(activeResult.timestampMs)}"
}

private data class AudioMvpSmokeTestState(
    val captureStartedAtMs: Long? = null,
    val captureStoppedAtMs: Long? = null,
    val energyEventAtMs: Long? = null,
    val voiceActivityStartedAtMs: Long? = null,
    val lastStimulusAtMs: Long? = null,
    val lastSoundEventType: EventType? = null,
    val lastSoundEventAtMs: Long? = null,
    val lastResponseRequest: AudioResponseRequestObservation? = null,
    val lastBehaviorResponseRequest: AudioResponseRequestObservation? = null,
    val lastPlaybackEvent: AudioPlaybackEventObservation? = null,
    val lastWakeWordDetection: KeywordDetectionObservation? = null,
    val lastKeywordDetection: KeywordDetectionObservation? = null
) {
    fun consume(event: EventEnvelope): AudioMvpSmokeTestState {
        return when (event.type) {
            EventType.AUDIO_CAPTURE_STARTED -> {
                copy(captureStartedAtMs = event.timestampMs)
            }
            EventType.AUDIO_CAPTURE_STOPPED -> {
                copy(captureStoppedAtMs = event.timestampMs)
            }
            EventType.SOUND_ENERGY_CHANGED -> {
                copy(energyEventAtMs = event.timestampMs)
            }
            EventType.SOUND_DETECTED -> {
                copy(
                    lastStimulusAtMs = event.timestampMs,
                    lastSoundEventType = event.type,
                    lastSoundEventAtMs = event.timestampMs
                )
            }
            EventType.VOICE_ACTIVITY_ENDED -> {
                copy(
                    lastSoundEventType = event.type,
                    lastSoundEventAtMs = event.timestampMs
                )
            }
            EventType.VOICE_ACTIVITY_STARTED -> {
                copy(
                    voiceActivityStartedAtMs = event.timestampMs,
                    lastStimulusAtMs = event.timestampMs,
                    lastSoundEventType = event.type,
                    lastSoundEventAtMs = event.timestampMs
                )
            }
            EventType.WAKE_WORD_DETECTED -> {
                copy(
                    lastStimulusAtMs = event.timestampMs,
                    lastSoundEventType = event.type,
                    lastSoundEventAtMs = event.timestampMs,
                    lastWakeWordDetection = event.toKeywordObservation()
                )
            }
            EventType.KEYWORD_DETECTED -> {
                copy(
                    lastStimulusAtMs = event.timestampMs,
                    lastSoundEventType = event.type,
                    lastSoundEventAtMs = event.timestampMs,
                    lastKeywordDetection = event.toKeywordObservation()
                )
            }
            EventType.AUDIO_RESPONSE_REQUESTED -> {
                val requestObservation = event.toRequestObservation()
                val behaviorRequestObservation = if (
                    isLikelyBehaviorResponseRequest(
                        requestObservation = requestObservation,
                        lastStimulusAtMs = lastStimulusAtMs
                    )
                ) {
                    requestObservation
                } else {
                    lastBehaviorResponseRequest
                }
                copy(
                    lastResponseRequest = requestObservation,
                    lastBehaviorResponseRequest = behaviorRequestObservation
                )
            }
            EventType.AUDIO_RESPONSE_STARTED,
            EventType.AUDIO_RESPONSE_COMPLETED,
            EventType.AUDIO_RESPONSE_SKIPPED -> {
                copy(lastPlaybackEvent = event.toPlaybackObservation())
            }
            else -> this
        }
    }
}

private data class AudioResponseRequestObservation(
    val category: String,
    val clipId: String?,
    val cooldownKey: String?,
    val timestampMs: Long,
    val payloadValid: Boolean
)

private data class AudioPlaybackEventObservation(
    val eventType: EventType,
    val category: String,
    val clipId: String?,
    val reason: String?,
    val timestampMs: Long,
    val payloadValid: Boolean
)

private data class KeywordDetectionObservation(
    val eventType: EventType,
    val keywordId: String,
    val keywordText: String?,
    val confidence: Float?,
    val engine: String?,
    val timestampMs: Long,
    val payloadValid: Boolean
)

private enum class SmokeCheckpointStatus {
    PASS,
    PENDING,
    NOT_APPLICABLE
}

private data class SmokeCheckpointEvaluation(
    val status: SmokeCheckpointStatus,
    val detail: String
)

private fun EventEnvelope.toRequestObservation(): AudioResponseRequestObservation {
    val payload = AudioResponseRequestPayload.fromJson(payloadJson)
    return AudioResponseRequestObservation(
        category = payload?.category ?: "MALFORMED",
        clipId = payload?.clipId,
        cooldownKey = payload?.cooldownKey,
        timestampMs = timestampMs,
        payloadValid = payload != null
    )
}

private fun EventEnvelope.toPlaybackObservation(): AudioPlaybackEventObservation {
    val payload = AudioResponsePayload.fromJson(payloadJson)
    return AudioPlaybackEventObservation(
        eventType = type,
        category = payload?.category ?: "MALFORMED",
        clipId = payload?.clipId,
        reason = payload?.reason,
        timestampMs = timestampMs,
        payloadValid = payload != null
    )
}

private fun EventEnvelope.toKeywordObservation(): KeywordDetectionObservation {
    val payload = KeywordDetectionPayload.fromJson(payloadJson)
    return KeywordDetectionObservation(
        eventType = type,
        keywordId = payload?.keywordId ?: "MALFORMED",
        keywordText = payload?.keywordText,
        confidence = payload?.confidence,
        engine = payload?.engine,
        timestampMs = timestampMs,
        payloadValid = payload != null
    )
}

private fun isLikelyBehaviorResponseRequest(
    requestObservation: AudioResponseRequestObservation,
    lastStimulusAtMs: Long?
): Boolean {
    if (!requestObservation.payloadValid) {
        return false
    }
    val stimulusTimestamp = lastStimulusAtMs ?: return false
    if (requestObservation.timestampMs < stimulusTimestamp) {
        return false
    }
    val elapsedSinceStimulusMs = requestObservation.timestampMs - stimulusTimestamp
    if (elapsedSinceStimulusMs > BEHAVIOR_REQUEST_SEQUENCE_WINDOW_MS) {
        return false
    }
    val normalizedCategory = requestObservation.category.trim().uppercase(Locale.US)
    return normalizedCategory in BEHAVIOR_RESPONSE_CATEGORIES
}

private fun evaluateWakeWordCheckpoint(
    config: KeywordSpottingConfig,
    runtimeState: KeywordSpotterState,
    runtimeNote: String?,
    wakeWordObservation: KeywordDetectionObservation?
): SmokeCheckpointEvaluation {
    if (!config.enabled) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.NOT_APPLICABLE,
            detail = "keyword spotting disabled"
        )
    }
    if (config.provider == KeywordSpottingProvider.NONE) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.NOT_APPLICABLE,
            detail = "provider NONE selected"
        )
    }
    if (runtimeState == KeywordSpotterState.FAILED) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.PENDING,
            detail = "adapter failed; check runtime note/error"
        )
    }
    if (runtimeNote?.contains("not integrated", ignoreCase = true) == true) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.NOT_APPLICABLE,
            detail = "selected provider not integrated"
        )
    }
    if (wakeWordObservation != null) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.PASS,
            detail = formatKeywordObservation(wakeWordObservation)
        )
    }
    return SmokeCheckpointEvaluation(
        status = SmokeCheckpointStatus.PENDING,
        detail = "waiting for ${EventType.WAKE_WORD_DETECTED.name}"
    )
}

private fun evaluateKeywordCheckpoint(
    keywordObservation: KeywordDetectionObservation?
): SmokeCheckpointEvaluation {
    if (keywordObservation == null) {
        return SmokeCheckpointEvaluation(
            status = SmokeCheckpointStatus.NOT_APPLICABLE,
            detail = "optional path; verify only if runtime emits ${EventType.KEYWORD_DETECTED.name}"
        )
    }
    return SmokeCheckpointEvaluation(
        status = SmokeCheckpointStatus.PASS,
        detail = formatKeywordObservation(keywordObservation)
    )
}

private fun formatSmokeCheckpoint(
    status: SmokeCheckpointStatus,
    label: String,
    detail: String
): String {
    val prefix = when (status) {
        SmokeCheckpointStatus.PASS -> "[OK]"
        SmokeCheckpointStatus.PENDING -> "[WAIT]"
        SmokeCheckpointStatus.NOT_APPLICABLE -> "[N/A]"
    }
    return "$prefix $label: $detail"
}

private fun formatLatestSmokeSoundEvent(
    runtimeDebugState: AudioRuntimeDebugState,
    smokeTestState: AudioMvpSmokeTestState
): String {
    val eventType = smokeTestState.lastSoundEventType ?: runtimeDebugState.lastSoundEventType
    val timestampMs = smokeTestState.lastSoundEventAtMs ?: runtimeDebugState.lastSoundEventTimestampMs
    return "${formatSoundEventType(eventType)} @ ${formatSoundEventTimestamp(timestampMs)}"
}

private fun formatRequestObservation(observation: AudioResponseRequestObservation?): String {
    val activeObservation = observation ?: return "-"
    return "category=${activeObservation.category}, clipId=${activeObservation.clipId ?: "-"}, " +
        "cooldownKey=${activeObservation.cooldownKey ?: "-"}, " +
        "validPayload=${if (activeObservation.payloadValid) "Yes" else "No"}, " +
        "timestamp=${formatSoundEventTimestamp(activeObservation.timestampMs)}"
}

private fun formatPlaybackObservation(observation: AudioPlaybackEventObservation?): String {
    val activeObservation = observation ?: return "-"
    return "event=${activeObservation.eventType.name}, category=${activeObservation.category}, " +
        "clipId=${activeObservation.clipId ?: "-"}, reason=${activeObservation.reason ?: "-"}, " +
        "validPayload=${if (activeObservation.payloadValid) "Yes" else "No"}, " +
        "timestamp=${formatSoundEventTimestamp(activeObservation.timestampMs)}"
}

private fun formatKeywordObservation(observation: KeywordDetectionObservation?): String {
    val activeObservation = observation ?: return "-"
    val confidenceLabel = activeObservation.confidence?.let {
        String.format(Locale.US, "%.3f", it)
    } ?: "-"
    return "event=${activeObservation.eventType.name}, keywordId=${activeObservation.keywordId}, " +
        "text=${activeObservation.keywordText ?: "-"}, confidence=$confidenceLabel, " +
        "engine=${activeObservation.engine ?: "-"}, " +
        "validPayload=${if (activeObservation.payloadValid) "Yes" else "No"}, " +
        "timestamp=${formatSoundEventTimestamp(activeObservation.timestampMs)}"
}

private const val TAG = "AudioDebugScreen"
private const val RUNTIME_POLL_INTERVAL_MS = 300L
private const val BEHAVIOR_REQUEST_SEQUENCE_WINDOW_MS = 10_000L
private val BEHAVIOR_RESPONSE_CATEGORIES: Set<String> = setOf(
    "ACKNOWLEDGMENT",
    "SURPRISED",
    "CURIOUS",
    "GREETING"
)
