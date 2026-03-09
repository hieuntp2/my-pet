package com.aipet.brain.app.ui.audio.model

sealed interface MicrophonePermissionState {
    object NotRequested : MicrophonePermissionState
    object Requesting : MicrophonePermissionState
    data class Denied(val canRequestAgain: Boolean) : MicrophonePermissionState
    object Granted : MicrophonePermissionState
}

enum class AudioReadinessState {
    NotReadyPermissionRequired,
    ReadyToInitialize,
    ReadyInitialized,
    NotReadyFailed
}

enum class AudioCaptureLifecycleState {
    Idle,
    Starting,
    Active,
    Stopped,
    Failed
}

data class AudioCaptureRuntimeDebugState(
    val isRunning: Boolean,
    val sampleRateHz: Int?,
    val channelCount: Int?,
    val frameSizeSamples: Int?
) {
    companion object {
        val Unknown = AudioCaptureRuntimeDebugState(
            isRunning = false,
            sampleRateHz = null,
            channelCount = null,
            frameSizeSamples = null
        )
    }
}

data class AudioDebugState(
    val permissionState: MicrophonePermissionState,
    val readinessState: AudioReadinessState,
    val captureState: AudioCaptureLifecycleState,
    val lastErrorMessage: String?,
    val captureRuntimeState: AudioCaptureRuntimeDebugState = AudioCaptureRuntimeDebugState.Unknown
)
