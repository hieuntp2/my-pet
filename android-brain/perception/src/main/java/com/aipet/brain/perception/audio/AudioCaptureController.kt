package com.aipet.brain.perception.audio

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.perception.audio.model.AudioCaptureState
import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.VadResult
import com.aipet.brain.perception.audio.model.VadState

class AudioCaptureController(
    private val config: AudioCaptureConfig = AudioCaptureConfig(),
    private val lifecycleListener: AudioCaptureLifecycleListener? = null,
    private val energyMetricsListener: AudioEnergyMetricsListener? = null,
    private val vadResultListener: AudioVadResultListener? = null
) {
    @Synchronized
    fun setFrameCallback(callback: ((AudioPcmFrame) -> Unit)?) {
        frameCallback = callback
    }

    @Synchronized
    fun setEnergyMetricsCallback(callback: ((AudioEnergyMetrics) -> Unit)?) {
        energyMetricsCallback = callback
    }

    @Synchronized
    fun setVadResultCallback(callback: ((VadResult) -> Unit)?) {
        vadResultCallback = callback
    }

    @Synchronized
    fun initialize(): AudioCaptureOperationResult {
        val result = audioFrameSource.initialize()
        return if (result.success) {
            success(result.message)
        } else {
            failure(result.message)
        }
    }

    @Synchronized
    fun startCapture(): AudioCaptureOperationResult {
        if (!audioFrameSource.isInitialized()) {
            return failure("Audio capture is not initialized.")
        }
        if (audioFrameSource.isRunning()) {
            Log.w(TAG, "Audio capture start requested while already active; ignoring duplicate start.")
            return success("Audio capture is already active.")
        }

        return try {
            processingLogWindowStartMs = SystemClock.elapsedRealtime()
            processedFramesSinceLog = 0
            energyEstimator.reset()
            vadDetector.reset()
            latestVadResult = null
            lastLoggedVadState = null

            val dispatcherStarted = audioProcessingDispatcher.start(
                onFrame = { frame ->
                    processFrame(frame)
                }
            )
            if (!dispatcherStarted) {
                return failure("Audio capture start failed: processing dispatcher failed to start.")
            }

            audioFrameSource.setFrameListener { frame ->
                audioProcessingDispatcher.dispatch(frame)
            }
            audioFrameSource.start()
            if (!audioFrameSource.isRunning()) {
                audioFrameSource.setFrameListener(null)
                audioProcessingDispatcher.stop()
                return failure(
                    audioFrameSource.lastErrorMessage()
                        ?: "Audio capture start failed: frame source did not enter running state."
                )
            }

            emitCaptureStartedEvent(timestampMs = System.currentTimeMillis())
            success("Audio capture started.")
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            failure("Audio capture start failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun stopCapture(): AudioCaptureOperationResult {
        if (!audioFrameSource.isInitialized()) {
            return failure("Audio capture is not initialized.")
        }
        if (!audioFrameSource.isRunning()) {
            Log.w(TAG, "Audio capture stop requested while already idle; ignoring duplicate stop.")
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            vadDetector.reset()
            latestVadResult = null
            lastLoggedVadState = null
            return failure("Audio capture is not active.")
        }

        return try {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            vadDetector.reset()
            latestVadResult = null
            lastLoggedVadState = null
            if (audioFrameSource.isRunning()) {
                failure("Audio capture stop failed: frame source is still running.")
            } else {
                emitCaptureStoppedEvent(timestampMs = System.currentTimeMillis())
                success("Audio capture stopped.")
            }
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            failure("Audio capture stop failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun release(): AudioCaptureOperationResult {
        val wasCapturing = audioFrameSource.isRunning()
        audioFrameSource.stop()
        audioFrameSource.setFrameListener(null)
        audioProcessingDispatcher.stop()
        vadDetector.reset()
        latestVadResult = null
        lastLoggedVadState = null
        if (wasCapturing && !audioFrameSource.isRunning()) {
            emitCaptureStoppedEvent(timestampMs = System.currentTimeMillis())
        }

        return try {
            val result = audioFrameSource.release()
            if (result.success) {
                latestEnergyMetrics = null
                success(result.message)
            } else {
                failure(result.message)
            }
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            failure("Audio release required forced cleanup: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun isInitialized(): Boolean = audioFrameSource.isInitialized()

    @Synchronized
    fun isCapturing(): Boolean = audioFrameSource.isRunning()

    @Synchronized
    fun latestEnergyMetrics(): AudioEnergyMetrics? = latestEnergyMetrics

    @Synchronized
    fun latestVadResult(): VadResult? = latestVadResult

    @Synchronized
    fun captureRuntimeState(): AudioCaptureState = audioFrameSource.captureState()

    private fun processFrame(frame: AudioFrame) {
        val metrics = energyEstimator.estimate(frame)
        val vadResult = vadDetector.processFrame(frame, metrics)
        val previousVadState = latestVadResult?.state
        latestEnergyMetrics = metrics
        latestVadResult = vadResult
        val legacyFrame = frame.toLegacyAudioPcmFrame()
        frameCallback?.invoke(legacyFrame)
        energyMetricsCallback?.invoke(metrics)
        val listener = energyMetricsListener
        if (listener != null) {
            try {
                listener.onEnergyMetrics(metrics)
            } catch (error: Throwable) {
                Log.e(TAG, "Audio energy metrics listener failed.", error)
            }
        }
        val vadCallback = vadResultCallback
        if (vadCallback != null) {
            try {
                vadCallback.invoke(vadResult)
            } catch (error: Throwable) {
                Log.e(TAG, "Audio VAD callback failed.", error)
            }
        }
        val vadListener = vadResultListener
        if (vadListener != null) {
            try {
                vadListener.onVadResult(vadResult)
            } catch (error: Throwable) {
                Log.e(TAG, "Audio VAD listener failed.", error)
            }
        }
        if (previousVadState != vadResult.state) {
            logVadTransition(
                previousState = previousVadState,
                result = vadResult,
                smoothedEnergy = metrics.smoothed
            )
        }
        processedFramesSinceLog += 1

        val nowMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowMs - processingLogWindowStartMs
        if (elapsedMs >= PROCESSING_LOG_WINDOW_MS) {
            val framesPerSecond = (processedFramesSinceLog * 1000f) / elapsedMs
            Log.d(
                TAG,
                "audio_energy_debug fps=$framesPerSecond " +
                    "rms=${formatMetric(metrics.rms)} " +
                    "peak=${formatMetric(metrics.peak)} " +
                    "smoothed=${formatMetric(metrics.smoothed)} " +
                    "level=${energyLevel(metrics.smoothed)} " +
                    "vad=${vadResult.state.name} " +
                    "sampleRate=${frame.sampleRate} frameSamples=${frame.sampleCount}"
            )
            processedFramesSinceLog = 0
            processingLogWindowStartMs = nowMs
        }
    }

    private fun formatMetric(value: Double): String {
        return String.format(java.util.Locale.US, "%.4f", value)
    }

    private fun energyLevel(smoothedRms: Double): String {
        return when {
            smoothedRms < QUIET_THRESHOLD -> "QUIET"
            smoothedRms < SPEECH_THRESHOLD -> "LOW"
            smoothedRms < LOUD_THRESHOLD -> "MEDIUM"
            else -> "HIGH"
        }
    }

    private fun logVadTransition(
        previousState: VadState?,
        result: VadResult,
        smoothedEnergy: Double
    ) {
        lastLoggedVadState = result.state
        Log.d(
            TAG,
            "vad_state_transition from=${previousState?.name ?: "NONE"} " +
                "to=${result.state.name} confidence=${formatMetric(result.confidence.toDouble())} " +
                "smoothed=${formatMetric(smoothedEnergy)} frameTs=${result.timestampMs}"
        )
    }

    private fun emitCaptureStartedEvent(timestampMs: Long) {
        val listener = lifecycleListener ?: return
        val lifecycleEvent = buildLifecycleEvent(timestampMs = timestampMs)
        try {
            listener.onCaptureStarted(lifecycleEvent)
            Log.d(
                TAG,
                "Audio capture lifecycle STARTED emitted. " +
                    "sampleRate=${lifecycleEvent.sampleRate} frameSize=${lifecycleEvent.frameSize} " +
                    "channelCount=${lifecycleEvent.channelCount}"
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Audio capture STARTED lifecycle listener failed.", error)
        }
    }

    private fun emitCaptureStoppedEvent(timestampMs: Long) {
        val listener = lifecycleListener ?: return
        val lifecycleEvent = buildLifecycleEvent(timestampMs = timestampMs)
        try {
            listener.onCaptureStopped(lifecycleEvent)
            Log.d(
                TAG,
                "Audio capture lifecycle STOPPED emitted. " +
                    "sampleRate=${lifecycleEvent.sampleRate} frameSize=${lifecycleEvent.frameSize} " +
                    "channelCount=${lifecycleEvent.channelCount}"
            )
        } catch (error: Throwable) {
            Log.e(TAG, "Audio capture STOPPED lifecycle listener failed.", error)
        }
    }

    private fun buildLifecycleEvent(timestampMs: Long): AudioCaptureLifecycleEvent {
        val state = audioFrameSource.captureState()
        return AudioCaptureLifecycleEvent(
            sampleRate = state.sampleRate,
            frameSize = state.frameSize,
            channelCount = state.channelCount,
            timestampMs = timestampMs
        )
    }

    private fun success(message: String): AudioCaptureOperationResult {
        Log.d(TAG, message)
        return AudioCaptureOperationResult(
            success = true,
            message = message
        )
    }

    private fun failure(message: String, error: Throwable? = null): AudioCaptureOperationResult {
        if (error != null) {
            Log.e(TAG, message, error)
        } else {
            Log.e(TAG, message)
        }
        return AudioCaptureOperationResult(
            success = false,
            message = message
        )
    }

    @Volatile
    private var frameCallback: ((AudioPcmFrame) -> Unit)? = null
    @Volatile
    private var energyMetricsCallback: ((AudioEnergyMetrics) -> Unit)? = null
    @Volatile
    private var vadResultCallback: ((VadResult) -> Unit)? = null
    @Volatile
    private var latestEnergyMetrics: AudioEnergyMetrics? = null
    @Volatile
    private var latestVadResult: VadResult? = null
    private val audioFrameSource = AudioRecordFrameSource(
        config = AudioRecordFrameSourceConfig(
            audioSource = config.audioSource,
            sampleRateHz = config.sampleRateHz,
            channelConfig = config.channelConfig,
            audioFormat = config.audioFormat,
            bufferMultiplier = config.bufferMultiplier,
            frameDurationMs = config.frameDurationMs
        )
    )
    private val audioProcessingDispatcher = AudioProcessingDispatcher()
    private val energyEstimator = EnergyEstimator()
    private val vadDetector: VoiceActivityDetector = VadLightStateMachine()
    private var processingLogWindowStartMs: Long = 0L
    private var processedFramesSinceLog: Int = 0
    private var lastLoggedVadState: VadState? = null

    companion object {
        private const val TAG = "AudioCaptureController"
        private const val PROCESSING_LOG_WINDOW_MS = 2_000L
        private const val QUIET_THRESHOLD = 0.03
        private const val SPEECH_THRESHOLD = 0.10
        private const val LOUD_THRESHOLD = 0.25
    }
}

data class AudioCaptureConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRateHz: Int = 16_000,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferMultiplier: Int = 2,
    val frameDurationMs: Int = 20
)

data class AudioCaptureOperationResult(
    val success: Boolean,
    val message: String
)

data class AudioCaptureLifecycleEvent(
    val sampleRate: Int,
    val frameSize: Int,
    val channelCount: Int,
    val timestampMs: Long
)

interface AudioCaptureLifecycleListener {
    fun onCaptureStarted(event: AudioCaptureLifecycleEvent)

    fun onCaptureStopped(event: AudioCaptureLifecycleEvent)
}

interface AudioEnergyMetricsListener {
    fun onEnergyMetrics(metrics: AudioEnergyMetrics)
}

interface AudioVadResultListener {
    fun onVadResult(result: VadResult)
}

private fun AudioFrame.toLegacyAudioPcmFrame(): AudioPcmFrame {
    return AudioPcmFrame(
        pcm16 = if (sampleCount == pcmData.size) {
            pcmData
        } else {
            pcmData.copyOf(sampleCount)
        },
        capturedAtElapsedRealtimeMs = timestampMs
    )
}

