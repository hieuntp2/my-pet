package com.aipet.brain.app.ui.audio

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.aipet.brain.app.ui.audio.model.AudioCaptureLifecycleState
import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.app.ui.audio.model.AudioDebugState
import com.aipet.brain.app.ui.audio.model.AudioReadinessState
import com.aipet.brain.app.ui.audio.model.MicrophonePermissionState
import com.aipet.brain.perception.audio.AudioCaptureController
import com.aipet.brain.perception.audio.AudioEnergyMetrics
import java.util.Locale

@Composable
fun AudioDebugScreen(
    hasRequestedPermission: Boolean,
    onPermissionRequestTracked: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mainHandler = remember { Handler(Looper.getMainLooper()) }
    val audioCaptureController = remember {
        AudioCaptureController()
    }
    val playbackEngine = remember(context.applicationContext) {
        AudioPlaybackEngine(context.applicationContext)
    }
    var latestEnergyMetrics by remember {
        mutableStateOf(audioCaptureController.latestEnergyMetrics())
    }
    var uiState by remember {
        mutableStateOf(
            createInitialAudioDebugState(
                permissionState = resolveMicrophonePermissionState(context, hasRequestedPermission)
            )
        )
    }

    fun updateState(nextState: AudioDebugState, reason: String) {
        val previousState = uiState
        logStateTransition(
            reason = reason,
            previous = previousState,
            next = nextState
        )
        uiState = nextState
    }

    fun playRandomClip(category: AudioCategory) {
        playbackEngine.playRandomClip(category)
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
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Audio Debug")
        Text(
            text = "Audio capture, energy, and sound debug (C96)",
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
            text = "Energy Metrics",
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
        )
        Text(
            text = "RMS: ${formatEnergyValue(latestEnergyMetrics?.rms)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Peak: ${formatEnergyValue(latestEnergyMetrics?.peakNormalized)}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Peak amplitude: ${latestEnergyMetrics?.peakAmplitude ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )
        Text(
            text = "Last error: ${uiState.lastErrorMessage ?: "-"}",
            modifier = Modifier.fillMaxWidth()
        )

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

        AudioCategory.entries.forEach { category ->
            Button(
                onClick = { playRandomClip(category) },
                enabled = AudioAssetRegistry.hasClips(category),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = category.label)
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

private fun createInitialAudioDebugState(permissionState: MicrophonePermissionState): AudioDebugState {
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
        }
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
            "lastError=${next.lastErrorMessage ?: "-"}"
    )
}

private fun formatEnergyValue(value: Double?): String {
    if (value == null) {
        return "-"
    }
    return String.format(Locale.US, "%.4f", value)
}

private const val TAG = "AudioDebugScreen"
