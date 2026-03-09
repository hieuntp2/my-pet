package com.aipet.brain.perception.audio

import android.os.SystemClock
import android.util.Log
import com.aipet.brain.perception.audio.model.AudioFrame
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class AudioProcessingDispatcher(
    private val queueCapacity: Int = 32
) {
    @Synchronized
    fun start(onFrame: (AudioFrame) -> Unit): Boolean {
        if (isRunning.get()) {
            Log.d(TAG, "Audio processing dispatcher is already running.")
            return true
        }

        queue.clear()
        droppedFramesSinceLog = 0
        droppedLogWindowStartMs = SystemClock.elapsedRealtime()
        isRunning.set(true)
        workerThread = Thread(
            {
                processLoop(onFrame)
            },
            THREAD_NAME
        ).apply { start() }
        Log.d(TAG, "Audio processing dispatcher started. queueCapacity=$queueCapacity")
        return true
    }

    fun dispatch(frame: AudioFrame): Boolean {
        if (!isRunning.get()) {
            return false
        }

        if (queue.offer(frame)) {
            return true
        }

        queue.poll()
        val enqueued = queue.offer(frame)
        droppedFramesSinceLog += 1
        maybeLogDroppedFrames()
        return enqueued
    }

    @Synchronized
    fun stop() {
        if (!isRunning.get()) {
            return
        }

        isRunning.set(false)
        workerThread?.interrupt()
        workerThread?.join(STOP_JOIN_TIMEOUT_MS)
        workerThread = null
        queue.clear()
        Log.d(TAG, "Audio processing dispatcher stopped.")
    }

    private fun processLoop(onFrame: (AudioFrame) -> Unit) {
        while (isRunning.get()) {
            val frame = try {
                queue.poll(POLL_TIMEOUT_MS, TimeUnit.MILLISECONDS)
            } catch (_: InterruptedException) {
                break
            } ?: continue

            try {
                onFrame(frame)
            } catch (error: Throwable) {
                Log.e(TAG, "Audio frame processing failed: ${error.message ?: "unknown error"}", error)
            }
        }
        Log.d(TAG, "Audio processing dispatcher loop stopped.")
    }

    private fun maybeLogDroppedFrames() {
        val nowMs = SystemClock.elapsedRealtime()
        val elapsedMs = nowMs - droppedLogWindowStartMs
        if (elapsedMs < DROPPED_LOG_WINDOW_MS) {
            return
        }

        if (droppedFramesSinceLog > 0) {
            Log.w(
                TAG,
                "Audio processing queue dropped frames count=$droppedFramesSinceLog in ${elapsedMs}ms"
            )
        }
        droppedFramesSinceLog = 0
        droppedLogWindowStartMs = nowMs
    }

    private val queue = ArrayBlockingQueue<AudioFrame>(queueCapacity)
    private val isRunning = AtomicBoolean(false)
    private var workerThread: Thread? = null
    private var droppedFramesSinceLog: Int = 0
    private var droppedLogWindowStartMs: Long = 0L

    companion object {
        private const val TAG = "AudioProcessingDispatcher"
        private const val THREAD_NAME = "AudioProcessingDispatcher"
        private const val POLL_TIMEOUT_MS = 100L
        private const val STOP_JOIN_TIMEOUT_MS = 300L
        private const val DROPPED_LOG_WINDOW_MS = 2_000L
    }
}
