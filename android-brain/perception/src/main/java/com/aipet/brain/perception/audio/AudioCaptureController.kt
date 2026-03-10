package com.aipet.brain.perception.audio

import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.perception.audio.keyword.KeywordDetectionListener
import com.aipet.brain.perception.audio.keyword.KeywordSpotter
import com.aipet.brain.perception.audio.model.AudioCaptureState
import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordSpotterState
import com.aipet.brain.perception.audio.model.VadResult
import com.aipet.brain.perception.audio.model.VadState

class AudioCaptureController(
    private val config: AudioCaptureConfig = AudioCaptureConfig(),
    private val lifecycleListener: AudioCaptureLifecycleListener? = null,
    private val energyMetricsListener: AudioEnergyMetricsListener? = null,
    private val vadResultListener: AudioVadResultListener? = null,
    private val keywordDetectionListener: AudioKeywordDetectionListener? = null,
    initialKeywordSpotter: KeywordSpotter? = null
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
    fun setKeywordSpotter(nextSpotter: KeywordSpotter?) {
        val currentSpotter = keywordSpotter
        if (currentSpotter === nextSpotter) {
            return
        }

        detachKeywordSpotter(currentSpotter)
        keywordSpotter = nextSpotter
        latestKeywordSpotterError = null
        latestKeywordDetection = null
        keywordDetectionTotal = 0L
        lastKeywordDetectionFingerprint = null
        latestKeywordSpotterState = nextSpotter?.state() ?: KeywordSpotterState.IDLE

        if (nextSpotter == null) {
            Log.d(TAG, "Keyword spotter detached.")
            return
        }

        nextSpotter.setDetectionListener(spotterDetectionListener)
        if (audioFrameSource.isRunning()) {
            startKeywordSpotter(nextSpotter)
        }
        latestKeywordSpotterState = nextSpotter.state()
        Log.d(
            TAG,
            "Keyword spotter attached. spotterId=${nextSpotter.spotterId} " +
                "state=${latestKeywordSpotterState.name}"
        )
    }

    @Synchronized
    fun latestKeywordDetectionResult(): KeywordDetectionResult? = latestKeywordDetection

    @Synchronized
    fun keywordDetectionCount(): Long = keywordDetectionTotal

    @Synchronized
    fun keywordSpotterState(): KeywordSpotterState = latestKeywordSpotterState

    @Synchronized
    fun keywordSpotterId(): String? = keywordSpotter?.spotterId

    @Synchronized
    fun keywordSpotterLastErrorMessage(): String? = latestKeywordSpotterError

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

            keywordSpotter?.let { spotter ->
                startKeywordSpotter(spotter)
            }
            emitCaptureStartedEvent(timestampMs = System.currentTimeMillis())
            success("Audio capture started.")
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            stopKeywordSpotter()
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
            stopKeywordSpotter()
            vadDetector.reset()
            latestVadResult = null
            lastLoggedVadState = null
            return failure("Audio capture is not active.")
        }

        return try {
            audioFrameSource.stop()
            audioFrameSource.setFrameListener(null)
            audioProcessingDispatcher.stop()
            stopKeywordSpotter()
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
            stopKeywordSpotter()
            failure("Audio capture stop failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun release(): AudioCaptureOperationResult {
        val wasCapturing = audioFrameSource.isRunning()
        audioFrameSource.stop()
        audioFrameSource.setFrameListener(null)
        audioProcessingDispatcher.stop()
        stopKeywordSpotter()
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
            stopKeywordSpotter()
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
        processKeywordFrame(frame)
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

    private fun processKeywordFrame(frame: AudioFrame) {
        val activeSpotter = keywordSpotter ?: return
        if (latestKeywordSpotterState != KeywordSpotterState.RUNNING) {
            return
        }

        try {
            val detection = activeSpotter.processFrame(frame) ?: return
            handleKeywordDetection(detection)
        } catch (error: Throwable) {
            latestKeywordSpotterError = "Keyword spotter runtime failure: ${error.message ?: "unknown error"}"
            Log.e(
                TAG,
                "Keyword spotter processing failed. spotterId=${activeSpotter.spotterId}",
                error
            )
            disableKeywordSpotter(activeSpotter)
        }
    }

    private fun startKeywordSpotter(spotter: KeywordSpotter) {
        try {
            spotter.setDetectionListener(spotterDetectionListener)
            spotter.start()
            latestKeywordSpotterState = spotter.state()
            latestKeywordSpotterError = null
            Log.d(
                TAG,
                "Keyword spotter started. spotterId=${spotter.spotterId} " +
                    "state=${latestKeywordSpotterState.name}"
            )
        } catch (error: Throwable) {
            latestKeywordSpotterState = KeywordSpotterState.FAILED
            latestKeywordSpotterError =
                "Keyword spotter start failed for ${spotter.spotterId}: ${error.message ?: "unknown error"}"
            Log.e(TAG, latestKeywordSpotterError, error)
            disableKeywordSpotter(spotter)
        }
    }

    private fun stopKeywordSpotter() {
        val activeSpotter = keywordSpotter ?: return
        try {
            activeSpotter.stop()
        } catch (error: Throwable) {
            latestKeywordSpotterError =
                "Keyword spotter stop failed for ${activeSpotter.spotterId}: ${error.message ?: "unknown error"}"
            Log.e(TAG, latestKeywordSpotterError, error)
        } finally {
            latestKeywordSpotterState = KeywordSpotterState.IDLE
        }
    }

    private fun disableKeywordSpotter(spotter: KeywordSpotter) {
        if (keywordSpotter !== spotter) {
            return
        }
        detachKeywordSpotter(spotter)
        keywordSpotter = null
        latestKeywordSpotterState = KeywordSpotterState.FAILED
    }

    private fun detachKeywordSpotter(spotter: KeywordSpotter?) {
        if (spotter == null) {
            return
        }
        try {
            spotter.setDetectionListener(null)
        } catch (error: Throwable) {
            Log.w(TAG, "Keyword spotter listener detach failed. spotterId=${spotter.spotterId}", error)
        }
        try {
            spotter.stop()
        } catch (error: Throwable) {
            Log.w(TAG, "Keyword spotter stop failed during detach. spotterId=${spotter.spotterId}", error)
        }
    }

    private fun handleKeywordDetection(result: KeywordDetectionResult) {
        val detectionFingerprint = buildString(capacity = 96) {
            append(result.keywordId)
            append("|")
            append(result.timestampMs)
            append("|")
            append(result.engineName)
        }
        if (lastKeywordDetectionFingerprint == detectionFingerprint) {
            return
        }

        lastKeywordDetectionFingerprint = detectionFingerprint
        latestKeywordDetection = result
        keywordDetectionTotal += 1L
        Log.d(
            TAG,
            "keyword_detected id=${result.keywordId} text=${result.keywordText ?: "-"} " +
                "confidence=${String.format(java.util.Locale.US, "%.3f", result.confidence)} " +
                "engine=${result.engineName} type=${result.detectionType.name} " +
                "count=$keywordDetectionTotal"
        )
        try {
            keywordDetectionListener?.onKeywordDetected(result)
        } catch (error: Throwable) {
            Log.e(TAG, "Audio keyword detection listener failed.", error)
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
    @Volatile
    private var keywordSpotter: KeywordSpotter? = initialKeywordSpotter
    @Volatile
    private var latestKeywordDetection: KeywordDetectionResult? = null
    @Volatile
    private var latestKeywordSpotterError: String? = null
    @Volatile
    private var latestKeywordSpotterState: KeywordSpotterState = initialKeywordSpotter?.state()
        ?: KeywordSpotterState.IDLE
    @Volatile
    private var keywordDetectionTotal: Long = 0L
    @Volatile
    private var lastKeywordDetectionFingerprint: String? = null
    private val spotterDetectionListener = KeywordDetectionListener { result ->
        handleKeywordDetection(result)
    }
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

interface AudioKeywordDetectionListener {
    fun onKeywordDetected(result: KeywordDetectionResult)
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

