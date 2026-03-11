package com.aipet.brain.perception.camera

import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aipet.brain.perception.vision.FaceDetectionPipeline
import java.util.concurrent.atomic.AtomicLong

class FrameAnalyzer(
    private val minEmitIntervalMs: Long = DEFAULT_EMIT_INTERVAL_MS,
    private val faceDetectionPipeline: FaceDetectionPipeline? = null,
    private val onDiagnostics: ((FrameDiagnostics) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private var lastEmittedAtMs: Long = 0L
    private val frameCounter = AtomicLong(1L)

    override fun analyze(imageProxy: ImageProxy) {
        val analyzedAtMs = System.currentTimeMillis()
        val processingStartedAtNs = SystemClock.elapsedRealtimeNanos()
        val frameId = frameCounter.getAndIncrement()

        try {
            faceDetectionPipeline?.processFrame(
                frameId = frameId,
                timestampMs = analyzedAtMs,
                imageProxy = imageProxy
            )

            val now = SystemClock.elapsedRealtime()
            if (now - lastEmittedAtMs >= minEmitIntervalMs) {
                val diagnostics = FrameDiagnostics(
                    timestampMs = analyzedAtMs,
                    width = imageProxy.width,
                    height = imageProxy.height,
                    rotationDegrees = imageProxy.imageInfo.rotationDegrees,
                    format = imageProxy.format,
                    processingDurationMs = (
                        (SystemClock.elapsedRealtimeNanos() - processingStartedAtNs) / NS_PER_MS
                    ).coerceAtLeast(1L)
                )
                onDiagnostics?.invoke(diagnostics)

                lastEmittedAtMs = now
                Log.d(
                    TAG,
                    "Frame: id=$frameId, ${diagnostics.width}x${diagnostics.height}, " +
                        "rotation=${diagnostics.rotationDegrees}, format=${diagnostics.format}, " +
                        "processingMs=${diagnostics.processingDurationMs}"
                )
            }
        } catch (error: Exception) {
            Log.e(TAG, "Frame analysis failed", error)
        } finally {
            imageProxy.close()
        }
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
        private const val DEFAULT_EMIT_INTERVAL_MS = 1_000L
        private const val NS_PER_MS = 1_000_000L
    }
}
