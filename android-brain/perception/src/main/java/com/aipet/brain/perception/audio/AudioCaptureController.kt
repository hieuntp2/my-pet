package com.aipet.brain.perception.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log

class AudioCaptureController(
    private val config: AudioCaptureConfig = AudioCaptureConfig()
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
    fun initialize(): AudioCaptureOperationResult {
        if (isInitialized()) {
            return success("Audio capture is already initialized.")
        }

        val minimumBufferSize = AudioRecord.getMinBufferSize(
            config.sampleRateHz,
            config.channelConfig,
            config.audioFormat
        )
        if (minimumBufferSize <= 0) {
            return failure("AudioRecord min buffer size is invalid: $minimumBufferSize")
        }
        val targetBufferSize = minimumBufferSize * config.bufferMultiplier

        return try {
            val createdRecord = AudioRecord(
                config.audioSource,
                config.sampleRateHz,
                config.channelConfig,
                config.audioFormat,
                targetBufferSize
            )
            if (createdRecord.state != AudioRecord.STATE_INITIALIZED) {
                createdRecord.release()
                return failure("AudioRecord failed to initialize.")
            }
            audioRecord = createdRecord
            success(
                "Audio capture initialized. sampleRateHz=${config.sampleRateHz}, " +
                    "bufferSize=$targetBufferSize"
            )
        } catch (error: SecurityException) {
            failure("Audio initialization failed: microphone permission missing.", error)
        } catch (error: IllegalArgumentException) {
            failure("Audio initialization failed: invalid AudioRecord config.", error)
        } catch (error: Throwable) {
            failure("Audio initialization failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun startCapture(): AudioCaptureOperationResult {
        val record = audioRecord ?: return failure("Audio capture is not initialized.")
        if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
            return success("Audio capture is already active.")
        }

        return try {
            processingLogWindowStartMs = SystemClock.elapsedRealtime()
            processedFramesSinceLog = 0
            record.startRecording()
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                val dispatcherStarted = audioProcessingDispatcher.start(
                    onFrame = { frame ->
                        processFrame(frame)
                    }
                )
                if (!dispatcherStarted) {
                    try {
                        record.stop()
                    } catch (_: Throwable) {
                        // The dispatcher failed to start, and stop is best-effort cleanup.
                    }
                    return failure("Audio capture started but processing dispatcher failed to start.")
                }
                val started = audioFrameSource.start(
                    audioRecord = record,
                    onFrame = { frame ->
                        audioProcessingDispatcher.dispatch(frame)
                    }
                )
                if (!started) {
                    try {
                        record.stop()
                    } catch (_: Throwable) {
                        // The frame source start failed, and stop is best-effort cleanup.
                    }
                    audioProcessingDispatcher.stop()
                    return failure("Audio capture started but frame source failed to start.")
                }
                success("Audio capture started.")
            } else {
                failure("Audio capture did not enter recording state.")
            }
        } catch (error: IllegalStateException) {
            failure("Audio capture start failed: invalid controller state.", error)
        } catch (error: SecurityException) {
            failure("Audio capture start failed: microphone permission missing.", error)
        } catch (error: Throwable) {
            failure("Audio capture start failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun stopCapture(): AudioCaptureOperationResult {
        val record = audioRecord ?: return failure("Audio capture is not initialized.")
        if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            return failure("Audio capture is not active.")
        }

        return try {
            record.stop()
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            success("Audio capture stopped.")
        } catch (error: IllegalStateException) {
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            failure("Audio capture stop failed: invalid controller state.", error)
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            failure("Audio capture stop failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun release(): AudioCaptureOperationResult {
        val record = audioRecord ?: return success("Audio capture is already released.")
        return try {
            if (record.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                record.stop()
            }
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            record.release()
            audioRecord = null
            success("Audio capture released.")
        } catch (error: IllegalStateException) {
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            record.release()
            audioRecord = null
            failure("Audio release required forced cleanup after stop failure.", error)
        } catch (error: Throwable) {
            audioFrameSource.stop()
            audioProcessingDispatcher.stop()
            record.release()
            audioRecord = null
            failure("Audio release required forced cleanup: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun isInitialized(): Boolean {
        val record = audioRecord ?: return false
        return record.state == AudioRecord.STATE_INITIALIZED
    }

    @Synchronized
    fun isCapturing(): Boolean {
        val record = audioRecord ?: return false
        return record.recordingState == AudioRecord.RECORDSTATE_RECORDING
    }

    @Synchronized
    fun latestEnergyMetrics(): AudioEnergyMetrics? {
        return latestEnergyMetrics
    }

    private fun processFrame(frame: AudioPcmFrame) {
        val metrics = audioEnergyEstimator.estimate(frame.pcm16)
        latestEnergyMetrics = metrics
        frameCallback?.invoke(frame)
        energyMetricsCallback?.invoke(metrics)
        processedFramesSinceLog += 1

        val nowMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowMs - processingLogWindowStartMs
        if (elapsedMs >= PROCESSING_LOG_WINDOW_MS) {
            val framesPerSecond = (processedFramesSinceLog * 1000f) / elapsedMs
            Log.d(
                TAG,
                "audio_energy_debug fps=$framesPerSecond " +
                    "rms=${metrics.rms} peakAmplitude=${metrics.peakAmplitude} " +
                    "peakNormalized=${metrics.peakNormalized}"
            )
            processedFramesSinceLog = 0
            processingLogWindowStartMs = nowMs
        }
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

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var frameCallback: ((AudioPcmFrame) -> Unit)? = null
    @Volatile
    private var energyMetricsCallback: ((AudioEnergyMetrics) -> Unit)? = null
    @Volatile
    private var latestEnergyMetrics: AudioEnergyMetrics? = null
    private val audioFrameSource = AudioFrameSource(
        sampleRateHz = config.sampleRateHz,
        channelCount = config.resolveChannelCount(),
        frameDurationMs = config.frameDurationMs
    )
    private val audioProcessingDispatcher = AudioProcessingDispatcher()
    private val audioEnergyEstimator = AudioEnergyEstimator()
    private var processingLogWindowStartMs: Long = 0L
    private var processedFramesSinceLog: Int = 0

    companion object {
        private const val TAG = "AudioCaptureController"
        private const val PROCESSING_LOG_WINDOW_MS = 2_000L
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

private fun AudioCaptureConfig.resolveChannelCount(): Int {
    return when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO -> 1
        AudioFormat.CHANNEL_IN_STEREO -> 2
        else -> 1
    }
}
