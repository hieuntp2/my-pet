package com.aipet.brain.app.ui.persons

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
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

        val cropRect = runCatching {
            detectPrimaryFaceRect(sourceBitmap)
        }.getOrNull()
        if (cropRect == null) {
            sourceBitmap.recycle()
            return@withContext FaceCropExtractionResult.NoFaceDetected
        }

        val croppedBitmap = runCatching {
            Bitmap.createBitmap(
                sourceBitmap,
                cropRect.left,
                cropRect.top,
                cropRect.width(),
                cropRect.height()
            )
        }.getOrNull()
        if (croppedBitmap == null) {
            sourceBitmap.recycle()
            return@withContext FaceCropExtractionResult.Failed("Unable to create face crop bitmap.")
        }

        val faceCropUri = runCatching {
            teachSampleImageStorage.createFaceCropImageUri(sampleCaptureId)
        }.getOrNull()
        if (faceCropUri == null) {
            sourceBitmap.recycle()
            croppedBitmap.recycle()
            return@withContext FaceCropExtractionResult.Failed("Unable to create face crop URI.")
        }

        val saved = runCatching {
            appContext.contentResolver.openOutputStream(faceCropUri)?.use { output ->
                croppedBitmap.compress(Bitmap.CompressFormat.JPEG, 92, output)
            } ?: false
        }.getOrDefault(false)

        sourceBitmap.recycle()
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

    private suspend fun detectPrimaryFaceRect(bitmap: Bitmap): Rect? {
        val options = FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_NONE)
            .build()
        val detector = FaceDetection.getClient(options)
        val image = InputImage.fromBitmap(bitmap, 0)

        return suspendCancellableCoroutine { continuation ->
            detector.process(image)
                .addOnSuccessListener { faces ->
                    if (faces.isEmpty()) {
                        continuation.resume(null)
                        return@addOnSuccessListener
                    }
                    val primary = faces.maxByOrNull { face ->
                        face.boundingBox.width() * face.boundingBox.height()
                    }
                    if (primary == null) {
                        continuation.resume(null)
                        return@addOnSuccessListener
                    }
                    val box = primary.boundingBox
                    val padding = (box.width() * 0.2f).toInt()
                    val left = (box.left - padding).coerceAtLeast(0)
                    val top = (box.top - padding).coerceAtLeast(0)
                    val right = (box.right + padding).coerceAtMost(bitmap.width)
                    val bottom = (box.bottom + padding).coerceAtMost(bitmap.height)
                    if (right <= left || bottom <= top) {
                        continuation.resume(null)
                    } else {
                        continuation.resume(Rect(left, top, right, bottom))
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
                .addOnCompleteListener {
                    detector.close()
                }
            continuation.invokeOnCancellation { detector.close() }
        }
    }
}
