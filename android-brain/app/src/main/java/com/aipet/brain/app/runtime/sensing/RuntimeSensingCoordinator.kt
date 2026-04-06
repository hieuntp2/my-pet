package com.aipet.brain.app.runtime.sensing

import com.aipet.brain.brain.attention.AttentionMode
import com.aipet.brain.brain.attention.FocusTargetType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class RuntimeSensingCoordinator(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val lock = Mutex()
    private val _state = MutableStateFlow(RuntimeSensingState.DEFAULT)

    val state: StateFlow<RuntimeSensingState> = _state.asStateFlow()

    private var lifecycleActive: Boolean = false
    private var cameraPermissionGranted: Boolean = false
    private var microphonePermissionGranted: Boolean = false
    private var attentionMode: AttentionMode? = null
    private var attentionIntensity: Float = 0f
    private var focusTargetType: FocusTargetType? = null
    private var lastTransitionReason: String = RuntimeSensingState.DEFAULT.lastTransitionReason
    private var lastTransitionAtMs: Long = RuntimeSensingState.DEFAULT.lastTransitionAtMs
    private var transitionCount: Int = RuntimeSensingState.DEFAULT.transitionCount
    private var lastErrorSummary: String? = null
    private val subsystemStates = RuntimeSensingSubsystem.entries.associateWith { subsystem ->
        RuntimeSensingSubsystemState(
            subsystem = subsystem,
            healthy = true,
            lastFailureAtMs = null,
            lastFailureMessage = null
        )
    }.toMutableMap()
    private val recentFailures = ArrayDeque<Long>()

    suspend fun onLifecycleActive(active: Boolean) {
        lock.withLock {
            lifecycleActive = active
            recomputeState(reason = if (active) "lifecycle_started" else "lifecycle_stopped")
        }
    }

    suspend fun onPermissions(
        cameraGranted: Boolean,
        microphoneGranted: Boolean
    ) {
        lock.withLock {
            cameraPermissionGranted = cameraGranted
            microphonePermissionGranted = microphoneGranted
            recomputeState(reason = "permission_state_changed")
        }
    }

    suspend fun onAttentionSignal(
        mode: AttentionMode?,
        intensity: Float,
        targetType: FocusTargetType?
    ) {
        lock.withLock {
            attentionMode = mode
            attentionIntensity = intensity.coerceIn(0f, 1f)
            focusTargetType = targetType
            recomputeState(reason = "attention_signal")
        }
    }

    suspend fun reportSubsystemFailure(
        subsystem: RuntimeSensingSubsystem,
        message: String
    ) {
        lock.withLock {
            val nowMs = nowProvider()
            subsystemStates[subsystem] = RuntimeSensingSubsystemState(
                subsystem = subsystem,
                healthy = false,
                lastFailureAtMs = nowMs,
                lastFailureMessage = message
            )
            lastErrorSummary = "${subsystem.name.lowercase()}: $message"
            recentFailures.addLast(nowMs)
            trimFailureWindow(nowMs)
            recomputeState(reason = "${subsystem.name.lowercase()}_failure")
        }
    }

    suspend fun reportSubsystemRecovered(subsystem: RuntimeSensingSubsystem) {
        lock.withLock {
            val previous = subsystemStates[subsystem]
            subsystemStates[subsystem] = RuntimeSensingSubsystemState(
                subsystem = subsystem,
                healthy = true,
                lastFailureAtMs = previous?.lastFailureAtMs,
                lastFailureMessage = previous?.lastFailureMessage
            )
            recomputeState(reason = "${subsystem.name.lowercase()}_recovered")
        }
    }

    fun currentState(): RuntimeSensingState = _state.value

    private fun recomputeState(reason: String) {
        val nowMs = nowProvider()
        trimFailureWindow(nowMs)
        val failureBurst = recentFailures.size >= FAILURE_BURST_THRESHOLD
        val cameraHealthy = subsystemStates[RuntimeSensingSubsystem.CAMERA]?.healthy != false
        val audioHealthy = subsystemStates[RuntimeSensingSubsystem.AUDIO]?.healthy != false

        var mode = resolveBaseMode()
        if (failureBurst) {
            mode = mode.degradedOneStep()
        }

        var cameraEnabled = lifecycleActive && cameraPermissionGranted && cameraHealthy && mode != RuntimeSensingMode.OFF
        var audioEnabled = lifecycleActive && microphonePermissionGranted && audioHealthy && mode != RuntimeSensingMode.OFF

        // If both sensing channels are unavailable, runtime sensing ownership is effectively off.
        if (!cameraEnabled && !audioEnabled) {
            mode = RuntimeSensingMode.OFF
        }

        val degradationReason = resolveDegradationReason(
            failureBurst = failureBurst,
            cameraHealthy = cameraHealthy,
            audioHealthy = audioHealthy,
            mode = mode
        )
        val cadence = resolveCadence(mode = mode, audioEnabled = audioEnabled)

        val previous = _state.value
        val transitioned = previous.mode != mode ||
            previous.cameraEnabled != cameraEnabled ||
            previous.audioEnabled != audioEnabled ||
            previous.degradationReason != degradationReason

        if (transitioned) {
            transitionCount += 1
            lastTransitionAtMs = nowMs
            lastTransitionReason = reason
        }

        _state.value = RuntimeSensingState(
            mode = mode,
            cadence = cadence,
            cameraEnabled = cameraEnabled,
            audioEnabled = audioEnabled,
            degraded = degradationReason != RuntimeSensingDegradationReason.NONE,
            degradationReason = degradationReason,
            lastTransitionReason = lastTransitionReason,
            lastTransitionAtMs = lastTransitionAtMs,
            transitionCount = transitionCount,
            subsystemStates = RuntimeSensingSubsystem.entries.map { subsystem ->
                subsystemStates[subsystem] ?: RuntimeSensingSubsystemState(
                    subsystem = subsystem,
                    healthy = true,
                    lastFailureAtMs = null,
                    lastFailureMessage = null
                )
            },
            lastErrorSummary = lastErrorSummary
        )
    }

    private fun resolveBaseMode(): RuntimeSensingMode {
        if (!lifecycleActive) {
            return RuntimeSensingMode.OFF
        }
        if (!cameraPermissionGranted && !microphonePermissionGranted) {
            return RuntimeSensingMode.OFF
        }

        val currentAttentionMode = attentionMode
        val intensity = attentionIntensity
        val target = focusTargetType

        return when {
            intensity >= FOCUS_INTENSITY_THRESHOLD ||
                (currentAttentionMode in FOCUS_ATTENTION_MODES &&
                    intensity >= ATTENTION_INTENSITY_THRESHOLD &&
                    target in FOCUS_TARGET_TYPES) -> RuntimeSensingMode.FOCUS

            intensity >= ATTENTION_INTENSITY_THRESHOLD ||
                currentAttentionMode in ATTENTION_MODES -> RuntimeSensingMode.ATTENTION

            else -> RuntimeSensingMode.PASSIVE_AWARENESS
        }
    }

    private fun resolveCadence(
        mode: RuntimeSensingMode,
        audioEnabled: Boolean
    ): RuntimeSensingCadence {
        val baseCadence = when (mode) {
            RuntimeSensingMode.OFF -> RuntimeSensingCadence.OFF
            RuntimeSensingMode.PASSIVE_AWARENESS -> RuntimeSensingCadence(
                faceCropIntervalMs = 2_400L,
                objectDetectionIntervalMs = 9_200L,
                audioEnabled = audioEnabled
            )
            RuntimeSensingMode.ATTENTION -> RuntimeSensingCadence(
                faceCropIntervalMs = 950L,
                objectDetectionIntervalMs = 3_400L,
                audioEnabled = audioEnabled
            )
            RuntimeSensingMode.FOCUS -> RuntimeSensingCadence(
                faceCropIntervalMs = 520L,
                objectDetectionIntervalMs = 1_800L,
                audioEnabled = audioEnabled
            )
        }
        return baseCadence.copy(audioEnabled = audioEnabled)
    }

    private fun resolveDegradationReason(
        failureBurst: Boolean,
        cameraHealthy: Boolean,
        audioHealthy: Boolean,
        mode: RuntimeSensingMode
    ): RuntimeSensingDegradationReason {
        if (!lifecycleActive) {
            return RuntimeSensingDegradationReason.LIFECYCLE_STOPPED
        }
        if (mode == RuntimeSensingMode.OFF && !cameraPermissionGranted && !microphonePermissionGranted) {
            return RuntimeSensingDegradationReason.CAMERA_PERMISSION_MISSING
        }
        if (!cameraPermissionGranted) {
            return RuntimeSensingDegradationReason.CAMERA_PERMISSION_MISSING
        }
        if (!microphonePermissionGranted) {
            return RuntimeSensingDegradationReason.MICROPHONE_PERMISSION_MISSING
        }
        if (!cameraHealthy) {
            return RuntimeSensingDegradationReason.CAMERA_FAILURE
        }
        if (!audioHealthy) {
            return RuntimeSensingDegradationReason.AUDIO_FAILURE
        }
        if (failureBurst) {
            return RuntimeSensingDegradationReason.FAILURE_BURST
        }
        return RuntimeSensingDegradationReason.NONE
    }

    private fun trimFailureWindow(nowMs: Long) {
        while (recentFailures.isNotEmpty() && nowMs - recentFailures.first() > FAILURE_BURST_WINDOW_MS) {
            recentFailures.removeFirst()
        }
    }

    private fun RuntimeSensingMode.degradedOneStep(): RuntimeSensingMode {
        return when (this) {
            RuntimeSensingMode.FOCUS -> RuntimeSensingMode.ATTENTION
            RuntimeSensingMode.ATTENTION -> RuntimeSensingMode.PASSIVE_AWARENESS
            RuntimeSensingMode.PASSIVE_AWARENESS -> RuntimeSensingMode.OFF
            RuntimeSensingMode.OFF -> RuntimeSensingMode.OFF
        }
    }

    private companion object {
        private const val ATTENTION_INTENSITY_THRESHOLD = 0.36f
        private const val FOCUS_INTENSITY_THRESHOLD = 0.72f
        private const val FAILURE_BURST_WINDOW_MS = 45_000L
        private const val FAILURE_BURST_THRESHOLD = 3

        private val ATTENTION_MODES = setOf(
            AttentionMode.PASSIVE_COMPANION,
            AttentionMode.CURIOUS_INSPECTION,
            AttentionMode.SOCIAL_LOCK,
            AttentionMode.LISTENING,
            AttentionMode.ALERT,
            AttentionMode.PLAY_FOCUS
        )

        private val FOCUS_ATTENTION_MODES = setOf(
            AttentionMode.SOCIAL_LOCK,
            AttentionMode.ALERT,
            AttentionMode.PLAY_FOCUS
        )

        private val FOCUS_TARGET_TYPES = setOf(
            FocusTargetType.USER_FACE,
            FocusTargetType.USER_VOICE,
            FocusTargetType.TOUCH_SOURCE,
            FocusTargetType.SOUND_SOURCE
        )
    }
}
