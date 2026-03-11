package com.aipet.brain.perception.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Rect
import android.graphics.YuvImage
import com.aipet.brain.perception.vision.model.FaceBoundingBox
import com.aipet.brain.perception.vision.model.FaceCropFailureReason
import com.aipet.brain.perception.vision.model.FaceCropResult
import java.io.ByteArrayOutputStream

class FaceCropper {

    fun cropFromBitmap(
        sourceBitmap: Bitmap,
        boundingBox: FaceBoundingBox
    ): FaceCropResult {
        if (sourceBitmap.isRecycled || sourceBitmap.width <= 0 || sourceBitmap.height <= 0) {
            return FaceCropResult.failure(FaceCropFailureReason.INVALID_SOURCE)
        }

        val cropBounds = clampBounds(
            imageWidth = sourceBitmap.width,
            imageHeight = sourceBitmap.height,
            boundingBox = boundingBox
        ) ?: return FaceCropResult.failure(FaceCropFailureReason.INVALID_BOUNDS)

        val cropWidth = cropBounds.width()
        val cropHeight = cropBounds.height()
        if (cropWidth <= 0 || cropHeight <= 0) {
            return FaceCropResult.failure(FaceCropFailureReason.INVALID_BOUNDS)
        }

        val rawCropBitmap = runCatching {
            Bitmap.createBitmap(sourceBitmap, cropBounds.left, cropBounds.top, cropWidth, cropHeight)
        }.getOrNull() ?: return FaceCropResult.failure(FaceCropFailureReason.CROP_FAILED)

        val croppedBitmap = if (rawCropBitmap === sourceBitmap) {
            rawCropBitmap.copy(rawCropBitmap.config, false)
                ?: return FaceCropResult.failure(FaceCropFailureReason.CROP_FAILED)
        } else {
            rawCropBitmap
        }

        return FaceCropResult.success(croppedBitmap)
    }

    fun cropFromNv21Frame(
        nv21Bytes: ByteArray,
        frameWidth: Int,
        frameHeight: Int,
        rotationDegrees: Int,
        boundingBox: FaceBoundingBox
    ): FaceCropResult {
        if (frameWidth <= 0 || frameHeight <= 0 || nv21Bytes.isEmpty()) {
            return FaceCropResult.failure(FaceCropFailureReason.INVALID_SOURCE)
        }

        val frameBitmap = decodeNv21ToBitmap(
            nv21Bytes = nv21Bytes,
            frameWidth = frameWidth,
            frameHeight = frameHeight
        ) ?: return FaceCropResult.failure(FaceCropFailureReason.FRAME_DECODE_FAILED)

        val rotatedBitmap = rotateBitmap(frameBitmap, rotationDegrees)
            ?: run {
                frameBitmap.recycle()
                return FaceCropResult.failure(FaceCropFailureReason.FRAME_ROTATION_FAILED)
            }

        val cropResult = cropFromBitmap(
            sourceBitmap = rotatedBitmap,
            boundingBox = boundingBox
        )

        if (!frameBitmap.isRecycled && frameBitmap !== rotatedBitmap) {
            frameBitmap.recycle()
        }
        if (!rotatedBitmap.isRecycled) {
            rotatedBitmap.recycle()
        }

        return cropResult
    }

    private fun clampBounds(
        imageWidth: Int,
        imageHeight: Int,
        boundingBox: FaceBoundingBox
    ): Rect? {
        if (imageWidth <= 0 || imageHeight <= 0) {
            return null
        }

        val minX = minOf(boundingBox.left, boundingBox.right)
        val maxX = maxOf(boundingBox.left, boundingBox.right)
        val minY = minOf(boundingBox.top, boundingBox.bottom)
        val maxY = maxOf(boundingBox.top, boundingBox.bottom)

        val left = minX.coerceIn(0, imageWidth - 1)
        val right = maxX.coerceIn(0, imageWidth)
        val top = minY.coerceIn(0, imageHeight - 1)
        val bottom = maxY.coerceIn(0, imageHeight)
        if (right <= left || bottom <= top) {
            return null
        }

        return Rect(left, top, right, bottom)
    }

    private fun decodeNv21ToBitmap(
        nv21Bytes: ByteArray,
        frameWidth: Int,
        frameHeight: Int
    ): Bitmap? {
        val yuvImage = runCatching {
            YuvImage(nv21Bytes, ImageFormat.NV21, frameWidth, frameHeight, null)
        }.getOrNull() ?: return null

        val outputStream = ByteArrayOutputStream()
        return try {
            val compressed = yuvImage.compressToJpeg(
                Rect(0, 0, frameWidth, frameHeight),
                JPEG_QUALITY,
                outputStream
            )
            if (!compressed) {
                return null
            }
            val jpegBytes = outputStream.toByteArray()
            BitmapFactory.decodeByteArray(jpegBytes, 0, jpegBytes.size)
        } catch (_: Exception) {
            null
        } finally {
            runCatching {
                outputStream.close()
            }
        }
    }

    private fun rotateBitmap(
        sourceBitmap: Bitmap,
        rotationDegrees: Int
    ): Bitmap? {
        val normalizedRotation = ((rotationDegrees % FULL_ROTATION_DEGREES) + FULL_ROTATION_DEGREES) %
            FULL_ROTATION_DEGREES
        if (normalizedRotation == 0) {
            return sourceBitmap
        }

        return runCatching {
            val matrix = Matrix().apply {
                postRotate(normalizedRotation.toFloat())
            }
            Bitmap.createBitmap(
                sourceBitmap,
                0,
                0,
                sourceBitmap.width,
                sourceBitmap.height,
                matrix,
                true
            )
        }.getOrNull()
    }

    companion object {
        private const val JPEG_QUALITY = 95
        private const val FULL_ROTATION_DEGREES = 360
    }
}
