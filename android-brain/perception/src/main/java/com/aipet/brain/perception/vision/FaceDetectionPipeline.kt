package com.aipet.brain.perception.vision

import android.graphics.Bitmap
import android.graphics.ImageFormat
import android.util.Log
import androidx.camera.core.ImageProxy
import com.aipet.brain.perception.vision.model.DetectedFace
import com.aipet.brain.perception.vision.model.FaceCropFailureReason
import com.aipet.brain.perception.vision.model.FaceCropResult
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import com.google.mlkit.vision.common.InputImage
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

class FaceDetectionPipeline(
    private val onFacesDetected: (FaceDetectionResult) -> Unit,
    private val onDetectorFailure: ((Throwable) -> Unit)? = null,
    private val onLiveFaceCropReady: ((faceCropBitmap: Bitmap, timestampMs: Long, cameraRotation: Int) -> Unit)? = null,
    private val liveFaceCropIntervalMs: Long = LIVE_CROP_INTERVAL_MS,
) {
    private val detectorExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val faceDetector = FaceDetector(callbackExecutor = detectorExecutor)
    private val faceCropper = FaceCropper()
    private val frameInFlight = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val detectorFailureReported = AtomicBoolean(false)
    private val captureSnapshotLock = Any()
    private var lastCropVerificationTimestampMs = 0L
    private var latestCaptureSnapshot: FaceCaptureSnapshot? = null
    private var lastLiveCropTimestampMs = 0L

    fun processFrame(
        frameId: Long,
        timestampMs: Long,
        imageProxy: ImageProxy
    ) {
        if (closed.get()) {
            return
        }
        if (!frameInFlight.compareAndSet(false, true)) {
            return
        }
        val frameWidth = imageProxy.width
        val frameHeight = imageProxy.height
        val rotationDegrees = imageProxy.imageInfo.rotationDegrees

        val frameData = runCatching {
            imageProxy.toMlKitFrameData()
        }.getOrElse { error ->
            Log.w(
                TAG,
                "Skipped face detection for frameId=$frameId: ${error.message ?: "unsupported frame"}",
                error
            )
            frameInFlight.set(false)
            publishResult(
                FaceDetectionResult(
                    frameId = frameId,
                    timestampMs = timestampMs,
                    frameWidth = frameWidth,
                    frameHeight = frameHeight,
                    rotationDegrees = rotationDegrees,
                    faces = emptyList()
                )
            )
            return
        }

        faceDetector.detect(
            image = frameData.inputImage,
            timestampMs = timestampMs,
            onSuccess = { faces ->
                updateCaptureSnapshot(
                    frameId = frameId,
                    timestampMs = timestampMs,
                    frameData = frameData,
                    faces = faces
                )
                publishResult(
                    FaceDetectionResult(
                        frameId = frameId,
                        timestampMs = timestampMs,
                        frameWidth = frameWidth,
                        frameHeight = frameHeight,
                        rotationDegrees = rotationDegrees,
                        faces = faces
                    )
                )
                logCropVerification(
                    frameId = frameId,
                    timestampMs = timestampMs,
                    frameData = frameData,
                    faces = faces
                )
                maybeEmitLiveFaceCrop(
                    timestampMs = timestampMs,
                    frameData = frameData,
                    faces = faces
                )
                Log.d(
                    TAG,
                    "Face detection completed. frameId=$frameId, faceCount=${faces.size}"
                )
            },
            onFailure = { error ->
                Log.w(
                    TAG,
                    "Face detection failed for frameId=$frameId: ${error.message ?: "unknown"}",
                    error
                )
                reportDetectorFailure(error)
                publishResult(
                    FaceDetectionResult(
                        frameId = frameId,
                        timestampMs = timestampMs,
                        frameWidth = frameWidth,
                        frameHeight = frameHeight,
                        rotationDegrees = rotationDegrees,
                        faces = emptyList()
                    )
                )
            },
            onComplete = {
                frameInFlight.set(false)
            }
        )
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) {
            return
        }
        faceDetector.close()
        detectorExecutor.shutdown()
    }

    fun captureLargestFaceSample(): FaceCropResult {
        val snapshot = synchronized(captureSnapshotLock) {
            latestCaptureSnapshot
        } ?: return FaceCropResult.failure(FaceCropFailureReason.NO_FRAME_AVAILABLE)

        // Multiple faces are resolved by selecting the largest area, which is usually the
        // clearest foreground sample for manual capture.
        val targetFace = snapshot.faces.maxByOrNull { face ->
            face.boundingBox.area()
        } ?: return FaceCropResult.failure(FaceCropFailureReason.NO_FACE_DETECTED)

        return faceCropper.cropFromNv21Frame(
            nv21Bytes = snapshot.nv21Bytes,
            frameWidth = snapshot.frameWidth,
            frameHeight = snapshot.frameHeight,
            rotationDegrees = snapshot.rotationDegrees,
            boundingBox = targetFace.boundingBox
        )
    }

    private fun maybeEmitLiveFaceCrop(
        timestampMs: Long,
        frameData: MlKitFrameData,
        faces: List<DetectedFace>
    ) {
        val callback = onLiveFaceCropReady ?: return
        if (faces.isEmpty()) return
        if (timestampMs - lastLiveCropTimestampMs < liveFaceCropIntervalMs) return
        val primaryFace = faces.maxByOrNull { it.boundingBox.area() } ?: return
        val cropResult = faceCropper.cropFromNv21Frame(
            nv21Bytes = frameData.nv21Bytes,
            frameWidth = frameData.frameWidth,
            frameHeight = frameData.frameHeight,
            rotationDegrees = frameData.rotationDegrees,
            boundingBox = primaryFace.boundingBox
        )
        val croppedBitmap = cropResult.bitmap ?: return
        lastLiveCropTimestampMs = timestampMs
        runCatching {
            callback(croppedBitmap, timestampMs, frameData.rotationDegrees)
        }.onFailure { error ->
            Log.w(TAG, "Live face crop callback threw an exception.", error)
            croppedBitmap.recycle()
        }
    }

    private fun publishResult(result: FaceDetectionResult) {
        runCatching {
            onFacesDetected(result)
        }.onFailure { error ->
            Log.e(
                TAG,
                "Face detection result callback failed for frameId=${result.frameId}",
                error
            )
        }
    }

    private fun reportDetectorFailure(error: Throwable) {
        if (!detectorFailureReported.compareAndSet(false, true)) {
            return
        }
        runCatching {
            onDetectorFailure?.invoke(error)
        }.onFailure { callbackError ->
            Log.e(
                TAG,
                "Face detection failure callback threw an exception.",
                callbackError
            )
        }
    }

    private fun updateCaptureSnapshot(
        frameId: Long,
        timestampMs: Long,
        frameData: MlKitFrameData,
        faces: List<DetectedFace>
    ) {
        synchronized(captureSnapshotLock) {
            latestCaptureSnapshot = FaceCaptureSnapshot(
                frameId = frameId,
                timestampMs = timestampMs,
                nv21Bytes = frameData.nv21Bytes,
                frameWidth = frameData.frameWidth,
                frameHeight = frameData.frameHeight,
                rotationDegrees = frameData.rotationDegrees,
                faces = faces.toList()
            )
        }
    }

    private fun logCropVerification(
        frameId: Long,
        timestampMs: Long,
        frameData: MlKitFrameData,
        faces: List<DetectedFace>
    ) {
        if (faces.isEmpty()) {
            return
        }
        if (timestampMs - lastCropVerificationTimestampMs < CROP_VERIFICATION_INTERVAL_MS) {
            return
        }

        lastCropVerificationTimestampMs = timestampMs
        val cropResult = faceCropper.cropFromNv21Frame(
            nv21Bytes = frameData.nv21Bytes,
            frameWidth = frameData.frameWidth,
            frameHeight = frameData.frameHeight,
            rotationDegrees = frameData.rotationDegrees,
            boundingBox = faces.first().boundingBox
        )

        if (cropResult.isSuccess) {
            Log.d(
                TAG,
                "Face crop verification succeeded. frameId=$frameId, " +
                    "crop=${cropResult.cropWidth}x${cropResult.cropHeight}"
            )
            cropResult.bitmap?.recycle()
        } else {
            Log.d(
                TAG,
                "Face crop verification skipped. frameId=$frameId, reason=${cropResult.failureReason}"
            )
        }
    }

    private fun ImageProxy.toMlKitFrameData(): MlKitFrameData {
        val nv21Bytes = when (format) {
            ImageFormat.YUV_420_888 -> toNv21Bytes()
            ImageFormat.NV21 -> toRawNv21Bytes()
            else -> throw IllegalArgumentException("Unsupported ImageProxy format: $format")
        }

        return MlKitFrameData(
            inputImage = InputImage.fromByteArray(
                nv21Bytes,
                width,
                height,
                imageInfo.rotationDegrees,
                InputImage.IMAGE_FORMAT_NV21
            ),
            nv21Bytes = nv21Bytes,
            frameWidth = width,
            frameHeight = height,
            rotationDegrees = imageInfo.rotationDegrees
        )
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
        private const val TAG = "FaceDetectionPipeline"
        private const val YUV_PLANE_COUNT = 3
        private const val LUMA_PLANE_INDEX = 0
        private const val U_PLANE_INDEX = 1
        private const val V_PLANE_INDEX = 2
        private const val CROP_VERIFICATION_INTERVAL_MS = 1_000L
        private const val LIVE_CROP_INTERVAL_MS = 1_000L
    }

    private data class MlKitFrameData(
        val inputImage: InputImage,
        val nv21Bytes: ByteArray,
        val frameWidth: Int,
        val frameHeight: Int,
        val rotationDegrees: Int
    )

    private data class FaceCaptureSnapshot(
        val frameId: Long,
        val timestampMs: Long,
        val nv21Bytes: ByteArray,
        val frameWidth: Int,
        val frameHeight: Int,
        val rotationDegrees: Int,
        val faces: List<DetectedFace>
    )
}

private fun com.aipet.brain.perception.vision.model.FaceBoundingBox.area(): Int {
    val width = (right - left).coerceAtLeast(0)
    val height = (bottom - top).coerceAtLeast(0)
    return width * height
}
