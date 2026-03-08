package com.aipet.brain.app.ui.persons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.PointF
import android.media.FaceDetector
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal sealed interface FaceCropExtractionResult {
    data class Success(val faceCropUri: String) : FaceCropExtractionResult
    data object NoFaceDetected : FaceCropExtractionResult
    data class Failed(val reason: String) : FaceCropExtractionResult
}

internal class TeachSampleFaceCropExtractor(
    private val appContext: Context,
    private val teachSampleImageStorage: TeachSampleImageStorage
) {
    suspend fun extractFaceCrop(
        sourceImageUri: String,
        sampleCaptureId: String
    ): FaceCropExtractionResult = withContext(Dispatchers.Default) {
        if (sourceImageUri.isBlank()) {
            return@withContext FaceCropExtractionResult.Failed("Source image URI is blank.")
        }

        val sourceBitmap = loadBitmapFromUri(sourceImageUri)
            ?: return@withContext FaceCropExtractionResult.Failed("Unable to load source image.")
        val detectionBitmap = sourceBitmap.toDetectionBitmap() ?: run {
            sourceBitmap.recycle()
            return@withContext FaceCropExtractionResult.Failed("Source image is invalid for face detection.")
        }

        val cropRect = runCatching {
            detectPrimaryFaceRect(detectionBitmap)
        }.getOrNull()
        if (cropRect == null) {
            sourceBitmap.recycle()
            detectionBitmap.recycle()
            return@withContext FaceCropExtractionResult.NoFaceDetected
        }

        val croppedBitmap = runCatching {
            Bitmap.createBitmap(
                detectionBitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width,
                cropRect.height
            )
        }.getOrNull()
        if (croppedBitmap == null) {
            sourceBitmap.recycle()
            detectionBitmap.recycle()
            return@withContext FaceCropExtractionResult.Failed("Unable to create face crop bitmap.")
        }

        val faceCropUri = runCatching {
            teachSampleImageStorage.createFaceCropImageUri(sampleCaptureId)
        }.getOrNull()
        if (faceCropUri == null) {
            sourceBitmap.recycle()
            detectionBitmap.recycle()
            croppedBitmap.recycle()
            return@withContext FaceCropExtractionResult.Failed("Unable to create face crop URI.")
        }

        val saved = runCatching {
            appContext.contentResolver.openOutputStream(faceCropUri)?.use { output ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            } ?: false
        }.getOrDefault(false)

        sourceBitmap.recycle()
        detectionBitmap.recycle()
        croppedBitmap.recycle()

        if (!saved) {
            return@withContext FaceCropExtractionResult.Failed("Unable to persist face crop image.")
        }
        FaceCropExtractionResult.Success(faceCropUri.toString())
    }

    private fun loadBitmapFromUri(imageUri: String): Bitmap? {
        val parsedUri = Uri.parse(imageUri)
        return runCatching {
            appContext.contentResolver.openInputStream(parsedUri)?.use { input ->
                BitmapFactory.decodeStream(input)
            } ?: when (parsedUri.scheme) {
                null, "file" -> BitmapFactory.decodeFile(parsedUri.path)
                else -> null
            }
        }.getOrNull()
    }

    private fun Bitmap.toDetectionBitmap(): Bitmap? {
        if (width < 2 || height < 2) {
            return null
        }
        val evenWidth = if (width % 2 == 0) width else width - 1
        if (evenWidth < 2) {
            return null
        }
        val baseBitmap = if (evenWidth == width) {
            this
        } else {
            Bitmap.createBitmap(this, 0, 0, evenWidth, height)
        }
        return runCatching {
            baseBitmap.copy(Bitmap.Config.RGB_565, true)
        }.getOrNull().also {
            if (baseBitmap !== this) {
                baseBitmap.recycle()
            }
        }
    }

    private fun detectPrimaryFaceRect(detectionBitmap: Bitmap): CropRect? {
        val faces = arrayOfNulls<FaceDetector.Face>(1)
        val detector = FaceDetector(
            detectionBitmap.width,
            detectionBitmap.height,
            faces.size
        )
        val faceCount = detector.findFaces(detectionBitmap, faces)
        if (faceCount <= 0) {
            return null
        }
        val primaryFace = faces.firstOrNull() ?: return null
        val eyeDistance = primaryFace.eyesDistance()
        if (eyeDistance <= 0f) {
            return null
        }

        val midpoint = PointF()
        primaryFace.getMidPoint(midpoint)
        if (midpoint.x.isNaN() || midpoint.y.isNaN()) {
            return null
        }

        val left = (midpoint.x - eyeDistance * 1.8f).toInt()
            .coerceIn(0, detectionBitmap.width - 1)
        val top = (midpoint.y - eyeDistance * 2.0f).toInt()
            .coerceIn(0, detectionBitmap.height - 1)
        val right = (midpoint.x + eyeDistance * 1.8f).toInt()
            .coerceIn(left + 1, detectionBitmap.width)
        val bottom = (midpoint.y + eyeDistance * 2.4f).toInt()
            .coerceIn(top + 1, detectionBitmap.height)
        val width = right - left
        val height = bottom - top
        if (width <= 1 || height <= 1) {
            return null
        }
        return CropRect(left = left, top = top, width = width, height = height)
    }
}

private data class CropRect(
    val left: Int,
    val top: Int,
    val width: Int,
    val height: Int
)
