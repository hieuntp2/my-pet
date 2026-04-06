package com.aipet.brain.app.runtime.sensing

enum class RuntimeSensingMode {
    OFF,
    PASSIVE_AWARENESS,
    ATTENTION,
    FOCUS
}

enum class RuntimeSensingSubsystem {
    CAMERA,
    AUDIO
}

enum class RuntimeSensingDegradationReason {
    NONE,
    LIFECYCLE_STOPPED,
    CAMERA_PERMISSION_MISSING,
    MICROPHONE_PERMISSION_MISSING,
    CAMERA_FAILURE,
    AUDIO_FAILURE,
    FAILURE_BURST
}

data class RuntimeSensingCadence(
    val faceCropIntervalMs: Long,
    val objectDetectionIntervalMs: Long,
    val audioEnabled: Boolean
) {
    companion object {
        val OFF = RuntimeSensingCadence(
            faceCropIntervalMs = Long.MAX_VALUE,
            objectDetectionIntervalMs = Long.MAX_VALUE,
            audioEnabled = false
        )
    }
}

data class RuntimeSensingSubsystemState(
    val subsystem: RuntimeSensingSubsystem,
    val healthy: Boolean,
    val lastFailureAtMs: Long?,
    val lastFailureMessage: String?
)

data class RuntimeSensingState(
    val mode: RuntimeSensingMode,
    val cadence: RuntimeSensingCadence,
    val cameraEnabled: Boolean,
    val audioEnabled: Boolean,
    val degraded: Boolean,
    val degradationReason: RuntimeSensingDegradationReason,
    val lastTransitionReason: String,
    val lastTransitionAtMs: Long,
    val transitionCount: Int,
    val subsystemStates: List<RuntimeSensingSubsystemState>,
    val lastErrorSummary: String?
) {
    companion object {
        val DEFAULT = RuntimeSensingState(
            mode = RuntimeSensingMode.OFF,
            cadence = RuntimeSensingCadence.OFF,
            cameraEnabled = false,
            audioEnabled = false,
            degraded = false,
            degradationReason = RuntimeSensingDegradationReason.NONE,
            lastTransitionReason = "initial_state",
            lastTransitionAtMs = 0L,
            transitionCount = 0,
            subsystemStates = RuntimeSensingSubsystem.entries.map { subsystem ->
                RuntimeSensingSubsystemState(
                    subsystem = subsystem,
                    healthy = true,
                    lastFailureAtMs = null,
                    lastFailureMessage = null
                )
            },
            lastErrorSummary = null
        )
    }
}
