package com.aipet.brain.perception.audio

import android.media.AudioRecord
import android.os.SystemClock
import android.util.Log
import java.util.concurrent.atomic.AtomicBoolean

class AudioFrameSource(
    sampleRateHz: Int,
    channelCount: Int,
    frameDurationMs: Int
) {
    private val frameSizeSamples: Int =
        ((sampleRateHz * frameDurationMs) / 1000).coerceAtLeast(1) * channelCount.coerceAtLeast(1)

    @Synchronized
    fun start(
        audioRecord: AudioRecord,
        onFrame: (AudioPcmFrame) -> Unit
    ): Boolean {
        if (isRunning.get()) {
            Log.d(TAG, "Audio frame source is already running.")
            return true
        }

        isRunning.set(true)
        readerThread = Thread(
            {
                readLoop(audioRecord = audioRecord, onFrame = onFrame)
            },
            THREAD_NAME
        ).apply { start() }

        Log.d(TAG, "Audio frame source started. frameSizeSamples=$frameSizeSamples")
        return true
    }

    @Synchronized
    fun stop() {
        if (!isRunning.get()) {
            return
        }
        isRunning.set(false)
        readerThread?.interrupt()
        readerThread = null
        Log.d(TAG, "Audio frame source stop requested.")
    }

    private fun readLoop(
        audioRecord: AudioRecord,
        onFrame: (AudioPcmFrame) -> Unit
    ) {
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
                        onFrame(
                            AudioPcmFrame(
                                pcm16 = frameBuffer.copyOf(),
                                capturedAtElapsedRealtimeMs = SystemClock.elapsedRealtime()
                            )
                        )
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
                    // Keep the loop alive if the driver returns no data for a cycle.
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
        Log.d(TAG, "Audio frame source loop stopped.")
    }

    private val isRunning = AtomicBoolean(false)
    private var readerThread: Thread? = null

    companion object {
        private const val TAG = "AudioFrameSource"
        private const val THREAD_NAME = "AudioFrameSourceReader"
        private const val FRAME_RATE_LOG_WINDOW_MS = 2_000L
    }
}

data class AudioPcmFrame(
    val pcm16: ShortArray,
    val capturedAtElapsedRealtimeMs: Long
)
