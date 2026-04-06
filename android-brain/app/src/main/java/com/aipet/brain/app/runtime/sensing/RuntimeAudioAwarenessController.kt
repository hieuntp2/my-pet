package com.aipet.brain.app.runtime.sensing

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.core.content.ContextCompat
import com.aipet.brain.app.audio.AudioCaptureLifecycleEventPublisher
import com.aipet.brain.perception.audio.AudioCaptureController
import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.VadResult
import com.aipet.brain.perception.audio.model.VadState
import java.util.concurrent.atomic.AtomicBoolean

internal class RuntimeAudioAwarenessController(
    context: Context,
    lifecyclePublisher: AudioCaptureLifecycleEventPublisher,
    private val onEnergySample: (AudioEnergyMetrics) -> Unit,
    private val onVoiceActivityChanged: (Boolean) -> Unit,
    private val onFailure: (String) -> Unit
) {
    private val appContext = context.applicationContext
    private val started = AtomicBoolean(false)
    private var lastVadState: VadState = VadState.SILENT

    private val captureController = AudioCaptureController(
        lifecycleListener = lifecyclePublisher,
        energyMetricsListener = lifecyclePublisher,
        vadResultListener = lifecyclePublisher,
        keywordDetectionListener = lifecyclePublisher
    ).apply {
        setEnergyMetricsCallback { metrics ->
            runCatching { onEnergySample(metrics) }
                .onFailure { error ->
                    Log.w(TAG, "Runtime audio energy callback failed.", error)
                }
        }
        setVadResultCallback { result ->
            handleVad(result)
        }
    }

    fun startPassiveAwareness(): Boolean {
        if (!isMicrophoneGranted()) {
            val message = "Microphone permission missing for runtime passive awareness."
            onFailure(message)
            return false
        }
        if (!started.compareAndSet(false, true)) {
            return true
        }

        val initResult = captureController.initialize()
        if (!initResult.success) {
            started.set(false)
            onFailure(initResult.message)
            return false
        }

        val startResult = captureController.startCapture()
        if (!startResult.success) {
            started.set(false)
            onFailure(startResult.message)
            return false
        }

        Log.i(TAG, "Runtime audio passive awareness started.")
        return true
    }

    fun stopPassiveAwareness() {
        if (!started.compareAndSet(true, false)) {
            return
        }
        runCatching {
            captureController.stopCapture()
            onVoiceActivityChanged(false)
            lastVadState = VadState.SILENT
            Log.i(TAG, "Runtime audio passive awareness stopped.")
        }.onFailure { error ->
            onFailure("Runtime audio stop failed: ${error.message ?: "unknown error"}")
        }
    }

    fun release() {
        stopPassiveAwareness()
        runCatching {
            captureController.release()
        }.onFailure { error ->
            Log.w(TAG, "Runtime audio release failed.", error)
        }
    }

    fun isRunning(): Boolean = started.get() && captureController.isCapturing()

    private fun handleVad(result: VadResult) {
        val previous = lastVadState
        val current = result.state
        lastVadState = current
        val enteredVoice = previous != VadState.VOICE_LIKELY && current == VadState.VOICE_LIKELY
        val exitedVoice = previous == VadState.VOICE_LIKELY && current != VadState.VOICE_LIKELY
        when {
            enteredVoice -> runCatching { onVoiceActivityChanged(true) }
                .onFailure { error ->
                    Log.w(TAG, "Runtime voice-start callback failed.", error)
                }
            exitedVoice -> runCatching { onVoiceActivityChanged(false) }
                .onFailure { error ->
                    Log.w(TAG, "Runtime voice-end callback failed.", error)
                }
            else -> Unit
        }
    }

    private fun isMicrophoneGranted(): Boolean {
        return ContextCompat.checkSelfPermission(
            appContext,
            Manifest.permission.RECORD_AUDIO
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        private const val TAG = "RuntimeAudioAware"
    }
}
