package com.aipet.brain.perception.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.SystemClock
import android.util.Log
import com.aipet.brain.perception.audio.model.AudioCaptureState
import com.aipet.brain.perception.audio.model.AudioFrame
import java.util.concurrent.atomic.AtomicBoolean

class AudioRecordFrameSource(
    private val config: AudioRecordFrameSourceConfig = AudioRecordFrameSourceConfig()
) : AudioFrameSource {
    @Synchronized
    fun initialize(): AudioRecordFrameSourceResult {
        if (isInitialized()) {
            return success("AudioRecord is already initialized.")
        }

        val channelCount = config.resolveChannelCount()
        val sampleRateCandidates = config.sampleRateCandidates()
        for (sampleRateHz in sampleRateCandidates) {
            val minimumBufferSize = AudioRecord.getMinBufferSize(
                sampleRateHz,
                config.channelConfig,
                config.audioFormat
            )
            if (minimumBufferSize <= 0) {
                Log.w(TAG, "Unsupported audio config at ${sampleRateHz}Hz. minBuffer=$minimumBufferSize")
                continue
            }

            val frameSizeSamples = ((sampleRateHz * config.frameDurationMs) / 1000)
                .coerceAtLeast(1) * channelCount
            val frameSizeBytes = frameSizeSamples * PCM_16BIT_BYTES
            val targetBufferSize = maxOf(
                minimumBufferSize * config.bufferMultiplier,
                frameSizeBytes * FRAME_BUFFER_HEADROOM_MULTIPLIER
            )

            try {
                val candidate = AudioRecord(
                    config.audioSource,
                    sampleRateHz,
                    config.channelConfig,
                    config.audioFormat,
                    targetBufferSize
                )
                if (candidate.state == AudioRecord.STATE_INITIALIZED) {
                    audioRecord = candidate
                    activeSampleRateHz = sampleRateHz
                    activeFrameSizeSamples = frameSizeSamples
                    activeChannelCount = channelCount
                    activeBufferSizeBytes = targetBufferSize
                    lastErrorMessage = null
                    return success(
                        "AudioRecord initialized. sampleRateHz=$sampleRateHz " +
                            "channelCount=$channelCount frameSizeSamples=$frameSizeSamples " +
                            "bufferSizeBytes=$targetBufferSize"
                    )
                }

                candidate.release()
                Log.w(TAG, "AudioRecord created but not initialized at ${sampleRateHz}Hz.")
            } catch (error: SecurityException) {
                return failure("AudioRecord init failed: microphone permission missing.", error)
            } catch (error: IllegalArgumentException) {
                Log.w(
                    TAG,
                    "AudioRecord init rejected at ${sampleRateHz}Hz: ${error.message ?: "unknown error"}",
                    error
                )
            } catch (error: Throwable) {
                Log.w(
                    TAG,
                    "AudioRecord init error at ${sampleRateHz}Hz: ${error.message ?: "unknown error"}",
                    error
                )
            }
        }

        return failure(
            "AudioRecord initialization failed for sample rates=${sampleRateCandidates.joinToString()}."
        )
    }

    @Synchronized
    override fun start() {
        if (isRunning.get()) {
            Log.d(TAG, "Audio frame source is already running.")
            return
        }

        val initResult = initialize()
        if (!initResult.success) {
            return
        }

        val record = audioRecord
        if (record == null) {
            failure("Audio frame source start failed: AudioRecord is null after initialize.")
            return
        }

        try {
            record.startRecording()
            if (record.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                failure("Audio frame source start failed: AudioRecord did not enter recording state.")
                return
            }

            isRunning.set(true)
            readerThread = Thread(
                { readLoop(audioRecord = record) },
                THREAD_NAME
            ).apply { start() }

            Log.d(
                TAG,
                "Audio frame source started. sampleRateHz=$activeSampleRateHz " +
                    "channelCount=$activeChannelCount frameSizeSamples=$activeFrameSizeSamples"
            )
        } catch (error: IllegalStateException) {
            stopRecordingSafely(record)
            failure("Audio frame source start failed: invalid AudioRecord state.", error)
        } catch (error: SecurityException) {
            stopRecordingSafely(record)
            failure("Audio frame source start failed: microphone permission missing.", error)
        } catch (error: Throwable) {
            stopRecordingSafely(record)
            failure("Audio frame source start failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    override fun stop() {
        if (!isRunning.get()) {
            stopRecordingSafely(audioRecord)
            return
        }

        isRunning.set(false)
        readerThread?.interrupt()
        readerThread?.join(STOP_JOIN_TIMEOUT_MS)
        readerThread = null
        stopRecordingSafely(audioRecord)
        Log.d(TAG, "Audio frame source stopped.")
    }

    @Synchronized
    fun release(): AudioRecordFrameSourceResult {
        stop()
        val record = audioRecord ?: return success("AudioRecord is already released.")

        return try {
            record.release()
            audioRecord = null
            activeSampleRateHz = 0
            activeChannelCount = 0
            activeFrameSizeSamples = 0
            activeBufferSizeBytes = 0
            success("AudioRecord released.")
        } catch (error: Throwable) {
            audioRecord = null
            activeSampleRateHz = 0
            activeChannelCount = 0
            activeFrameSizeSamples = 0
            activeBufferSizeBytes = 0
            failure("AudioRecord release failed: ${error.message ?: "unknown error"}", error)
        }
    }

    @Synchronized
    fun isInitialized(): Boolean {
        val record = audioRecord ?: return false
        return record.state == AudioRecord.STATE_INITIALIZED
    }

    override fun isRunning(): Boolean = isRunning.get()

    override fun captureState(): AudioCaptureState {
        return AudioCaptureState(
            isRunning = isRunning(),
            sampleRate = activeSampleRateHz,
            channelCount = activeChannelCount,
            frameSize = activeFrameSizeSamples
        )
    }

    override fun setFrameListener(listener: ((AudioFrame) -> Unit)?) {
        frameListener = listener
    }

    fun lastErrorMessage(): String? = lastErrorMessage

    fun bufferSizeBytes(): Int = activeBufferSizeBytes

    private fun stopRecordingSafely(audioRecord: AudioRecord?) {
        if (audioRecord == null) {
            return
        }
        if (audioRecord.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
            return
        }
        try {
            audioRecord.stop()
        } catch (error: Throwable) {
            Log.w(TAG, "AudioRecord stop failed during cleanup.", error)
        }
    }

    private fun readLoop(
        audioRecord: AudioRecord
    ) {
        val frameSizeSamples = activeFrameSizeSamples
        if (frameSizeSamples <= 0) {
            failure("Audio frame source read loop aborted: invalid frame size.")
            isRunning.set(false)
            return
        }

        val frameBuffer = ShortArray(frameSizeSamples)
        var filledSamples = 0
        var framesInLogWindow = 0
        var windowStartMs = SystemClock.elapsedRealtime()

        while (isRunning.get()) {
            val remainingSamples = frameSizeSamples - filledSamples
            val readSamples = audioRecord.read(
                frameBuffer,
                filledSamples,
                remainingSamples,
                AudioRecord.READ_BLOCKING
            )
            when {
                readSamples > 0 -> {
                    filledSamples += readSamples
                    if (filledSamples == frameSizeSamples) {
                        val frame = AudioFrame(
                            timestampMs = SystemClock.elapsedRealtime(),
                            sampleRate = activeSampleRateHz,
                            channelCount = activeChannelCount,
                            sampleCount = frameSizeSamples,
                            pcmData = frameBuffer.copyOf()
                        )
                        try {
                            frameListener?.invoke(frame)
                        } catch (error: Throwable) {
                            Log.e(
                                TAG,
                                "Audio frame callback failed: ${error.message ?: "unknown error"}",
                                error
                            )
                        }

                        framesInLogWindow += 1
                        filledSamples = 0

                        val nowMs = SystemClock.elapsedRealtime()
                        val elapsedMs = nowMs - windowStartMs
                        if (elapsedMs >= FRAME_RATE_LOG_WINDOW_MS) {
                            val framesPerSecond = (framesInLogWindow * 1000f) / elapsedMs
                            Log.d(
                                TAG,
                                "Audio frame rate fps=$framesPerSecond, frameSizeSamples=$frameSizeSamples"
                            )
                            framesInLogWindow = 0
                            windowStartMs = nowMs
                        }
                    }
                }

                readSamples == 0 -> {
                    Log.w(TAG, "AudioRecord returned 0 samples; continuing read loop.")
                }

                readSamples == AudioRecord.ERROR_DEAD_OBJECT -> {
                    Log.e(TAG, "Audio frame read failed: dead audio object.")
                    break
                }

                readSamples == AudioRecord.ERROR_INVALID_OPERATION -> {
                    if (isRunning.get()) {
                        Log.e(TAG, "Audio frame read failed: invalid operation.")
                    }
                    break
                }

                readSamples == AudioRecord.ERROR_BAD_VALUE -> {
                    Log.e(TAG, "Audio frame read failed: bad read request.")
                    break
                }

                else -> {
                    Log.e(TAG, "Audio frame read failed with code=$readSamples")
                    break
                }
            }
        }

        isRunning.set(false)
        stopRecordingSafely(audioRecord)
        Log.d(TAG, "Audio frame source loop stopped.")
    }

    private fun success(message: String): AudioRecordFrameSourceResult {
        Log.d(TAG, message)
        return AudioRecordFrameSourceResult(
            success = true,
            message = message
        )
    }

    private fun failure(message: String, error: Throwable? = null): AudioRecordFrameSourceResult {
        if (error == null) {
            Log.e(TAG, message)
        } else {
            Log.e(TAG, message, error)
        }
        lastErrorMessage = message
        return AudioRecordFrameSourceResult(
            success = false,
            message = message
        )
    }

    private var audioRecord: AudioRecord? = null
    @Volatile
    private var frameListener: ((AudioFrame) -> Unit)? = null
    private val isRunning = AtomicBoolean(false)
    private var readerThread: Thread? = null
    @Volatile
    private var lastErrorMessage: String? = null
    @Volatile
    private var activeSampleRateHz: Int = 0
    @Volatile
    private var activeChannelCount: Int = 0
    @Volatile
    private var activeFrameSizeSamples: Int = 0
    @Volatile
    private var activeBufferSizeBytes: Int = 0

    companion object {
        private const val TAG = "AudioRecordFrameSource"
        private const val THREAD_NAME = "AudioFrameSourceReader"
        private const val FRAME_RATE_LOG_WINDOW_MS = 2_000L
        private const val STOP_JOIN_TIMEOUT_MS = 300L
        private const val PCM_16BIT_BYTES = 2
        private const val FRAME_BUFFER_HEADROOM_MULTIPLIER = 2
    }
}

data class AudioRecordFrameSourceConfig(
    val audioSource: Int = MediaRecorder.AudioSource.MIC,
    val sampleRateHz: Int = 16_000,
    val fallbackSampleRateHz: Int = 44_100,
    val channelConfig: Int = AudioFormat.CHANNEL_IN_MONO,
    val audioFormat: Int = AudioFormat.ENCODING_PCM_16BIT,
    val bufferMultiplier: Int = 2,
    val frameDurationMs: Int = 20
)

data class AudioRecordFrameSourceResult(
    val success: Boolean,
    val message: String
)

private fun AudioRecordFrameSourceConfig.resolveChannelCount(): Int {
    return when (channelConfig) {
        AudioFormat.CHANNEL_IN_MONO -> 1
        AudioFormat.CHANNEL_IN_STEREO -> 2
        else -> 1
    }
}

private fun AudioRecordFrameSourceConfig.sampleRateCandidates(): IntArray {
    return if (sampleRateHz == fallbackSampleRateHz) {
        intArrayOf(sampleRateHz)
    } else {
        intArrayOf(sampleRateHz, fallbackSampleRateHz)
    }
}

data class AudioPcmFrame(
    val pcm16: ShortArray,
    val capturedAtElapsedRealtimeMs: Long
)
