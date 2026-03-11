package com.aipet.brain.perception.camera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import android.os.SystemClock
import android.util.Log
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.aipet.brain.perception.vision.FaceDetectionPipeline
import com.aipet.brain.perception.vision.objectdetection.ObjectDetectionEngine
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicLong

class FrameAnalyzer(
    private val minEmitIntervalMs: Long = DEFAULT_EMIT_INTERVAL_MS,
    private val faceDetectionPipeline: FaceDetectionPipeline? = null,
    private val onDiagnostics: ((FrameDiagnostics) -> Unit)? = null,
    private val objectDetectionEngine: ObjectDetectionEngine? = null,
    private val minObjectDetectionIntervalMs: Long = DEFAULT_OBJECT_DETECTION_INTERVAL_MS,
    private val onObjectDetectionResult: ((Result<ObjectDetectionResult>) -> Unit)? = null
) : ImageAnalysis.Analyzer {

    private var lastEmittedAtMs: Long = 0L
    private var lastObjectDetectionAtMs: Long = 0L
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
            maybeDetectObjects(
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

    private fun maybeDetectObjects(
        timestampMs: Long,
        imageProxy: ImageProxy
    ) {
        val engine = objectDetectionEngine ?: return
        val callback = onObjectDetectionResult ?: return
        val now = SystemClock.elapsedRealtime()
        if (now - lastObjectDetectionAtMs < minObjectDetectionIntervalMs) {
            return
        }
        lastObjectDetectionAtMs = now

        val frameBitmap = runCatching {
            imageProxy.toFrameBitmap()
        }.getOrElse { error ->
            val detectionFailure = Result.failure<ObjectDetectionResult>(error)
            Log.w(
                TAG,
                "Object detection frame conversion failed: ${error.message ?: "unknown error"}",
                error
            )
            dispatchObjectDetectionResult(
                callback = callback,
                result = detectionFailure
            )
            return
        }

        val detectionResult = try {
            engine.detectObjects(
                frameBitmap = frameBitmap,
                timestampMs = timestampMs,
                rotationDegrees = imageProxy.imageInfo.rotationDegrees
            )
        } finally {
            if (!frameBitmap.isRecycled) {
                frameBitmap.recycle()
            }
        }

        detectionResult.onSuccess { result ->
            val topDetection = result.detections.firstOrNull()
            if (topDetection == null) {
                Log.d(TAG, "Object detection: no detections.")
            } else {
                Log.d(
                    TAG,
                    "Object detection: topLabel=${topDetection.label}, " +
                        "confidence=${"%.3f".format(java.util.Locale.US, topDetection.confidence)}"
                )
            }
        }.onFailure { error ->
            Log.w(
                TAG,
                "Object detection inference failed: ${error.message ?: "unknown error"}",
                error
            )
        }

        dispatchObjectDetectionResult(
            callback = callback,
            result = detectionResult
        )
    }

    private fun dispatchObjectDetectionResult(
        callback: (Result<ObjectDetectionResult>) -> Unit,
        result: Result<ObjectDetectionResult>
    ) {
        runCatching {
            callback(result)
        }.onFailure { callbackError ->
            Log.e(TAG, "Object detection callback failed.", callbackError)
        }
    }

    private fun ImageProxy.toFrameBitmap(): Bitmap {
        val nv21Bytes = when (format) {
            ImageFormat.YUV_420_888 -> toNv21Bytes()
            ImageFormat.NV21 -> toRawNv21Bytes()
            else -> throw IllegalArgumentException("Unsupported ImageProxy format: $format")
        }

        val yuvImage = YuvImage(nv21Bytes, ImageFormat.NV21, width, height, null)
        val jpegBuffer = ByteArrayOutputStream()
        check(
            yuvImage.compressToJpeg(
                Rect(0, 0, width, height),
                FRAME_JPEG_QUALITY,
                jpegBuffer
            )
        ) {
            "Unable to compress camera frame to JPEG for object detection."
        }
        val jpegBytes = jpegBuffer.toByteArray()
        return BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
            ?: throw IllegalStateException("Unable to decode camera frame JPEG to bitmap.")
    }

    private fun ImageProxy.toRawNv21Bytes(): ByteArray {
        val plane = planes.firstOrNull()
            ?: throw IllegalStateException("NV21 frame does not contain any plane data.")
        return plane.buffer.toByteArray()
    }

    private fun ImageProxy.toNv21Bytes(): ByteArray {
        if (planes.size < YUV_PLANE_COUNT) {
            throw IllegalStateException("YUV_420_888 frame has insufficient planes: ${planes.size}")
        }

        val frameWidth = width
        val frameHeight = height
        val ySize = frameWidth * frameHeight
        val output = ByteArray(ySize + ySize / 2)

        copyLumaPlane(
            plane = planes[LUMA_PLANE_INDEX],
            width = frameWidth,
            height = frameHeight,
            output = output
        )
        interleaveChromaPlanesAsNv21(
            uPlane = planes[U_PLANE_INDEX],
            vPlane = planes[V_PLANE_INDEX],
            width = frameWidth,
            height = frameHeight,
            output = output,
            outputOffset = ySize
        )
        return output
    }

    private fun copyLumaPlane(
        plane: ImageProxy.PlaneProxy,
        width: Int,
        height: Int,
        output: ByteArray
    ) {
        val buffer = plane.buffer
        val rowStride = plane.rowStride
        val pixelStride = plane.pixelStride
        var outputIndex = 0

        for (row in 0 until height) {
            val rowStart = row * rowStride
            for (column in 0 until width) {
                output[outputIndex++] = buffer.get(rowStart + column * pixelStride)
            }
        }
    }

    private fun interleaveChromaPlanesAsNv21(
        uPlane: ImageProxy.PlaneProxy,
        vPlane: ImageProxy.PlaneProxy,
        width: Int,
        height: Int,
        output: ByteArray,
        outputOffset: Int
    ) {
        val chromaWidth = width / 2
        val chromaHeight = height / 2
        val uBuffer = uPlane.buffer
        val vBuffer = vPlane.buffer
        val uRowStride = uPlane.rowStride
        val vRowStride = vPlane.rowStride
        val uPixelStride = uPlane.pixelStride
        val vPixelStride = vPlane.pixelStride
        var outputIndex = outputOffset

        for (row in 0 until chromaHeight) {
            val uRowStart = row * uRowStride
            val vRowStart = row * vRowStride
            for (column in 0 until chromaWidth) {
                output[outputIndex++] = vBuffer.get(vRowStart + column * vPixelStride)
                output[outputIndex++] = uBuffer.get(uRowStart + column * uPixelStride)
            }
        }
    }

    private fun ByteBuffer.toByteArray(): ByteArray {
        val duplicate = duplicate()
        duplicate.rewind()
        return ByteArray(duplicate.remaining()).also { bytes ->
            duplicate.get(bytes)
        }
    }

    companion object {
        private const val TAG = "FrameAnalyzer"
        private const val DEFAULT_EMIT_INTERVAL_MS = 1_000L
        private const val DEFAULT_OBJECT_DETECTION_INTERVAL_MS = 700L
        private const val YUV_PLANE_COUNT = 3
        private const val LUMA_PLANE_INDEX = 0
        private const val U_PLANE_INDEX = 1
        private const val V_PLANE_INDEX = 2
        private const val FRAME_JPEG_QUALITY = 90
        private const val NS_PER_MS = 1_000_000L
    }
}
