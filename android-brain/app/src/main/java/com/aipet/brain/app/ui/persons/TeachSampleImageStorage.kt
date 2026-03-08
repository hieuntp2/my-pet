package com.aipet.brain.app.ui.persons

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

internal class TeachSampleImageStorage(
    private val appContext: Context
) {
    fun createCameraCaptureImageUri(sampleCaptureId: String): Uri {
        return createManagedImageUri(
            sampleCaptureId = sampleCaptureId,
            relativeDirectoryPath = "teach_samples/camera"
        )
    }

    fun createFaceCropImageUri(sampleCaptureId: String): Uri {
        return createManagedImageUri(
            sampleCaptureId = sampleCaptureId,
            relativeDirectoryPath = "teach_samples/face_crops"
        )
    }

    private fun createManagedImageUri(
        sampleCaptureId: String,
        relativeDirectoryPath: String
    ): Uri {
        val normalizedCaptureId = sampleCaptureId.trim()
        require(normalizedCaptureId.isNotBlank()) {
            "sampleCaptureId cannot be blank."
        }
        val safeCaptureId = normalizedCaptureId.replace(Regex("[^A-Za-z0-9._-]"), "_")
        val samplesDir = File(appContext.filesDir, relativeDirectoryPath)
        if (!samplesDir.exists()) {
            check(samplesDir.mkdirs()) {
                "Unable to create teach sample image directory."
            }
        }
        val outputFile = File(samplesDir, "$safeCaptureId.jpg")
        if (outputFile.exists()) {
            check(outputFile.delete()) {
                "Unable to replace existing teach sample image."
            }
        }
        if (!outputFile.exists()) {
            check(outputFile.createNewFile()) {
                "Unable to create teach sample image file."
            }
        }
        return FileProvider.getUriForFile(
            appContext,
            "${appContext.packageName}.fileprovider",
            outputFile
        )
    }
}
