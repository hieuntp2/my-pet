package com.aipet.brain.perception.vision

import android.util.Log
import com.aipet.brain.perception.vision.model.DetectedFace
import com.aipet.brain.perception.vision.model.FaceBoundingBox
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import java.util.concurrent.Executor

class FaceDetector(
    private val callbackExecutor: Executor
) {
    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setContourMode(FaceDetectorOptions.CONTOUR_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .enableTracking()
            .build()
    )

    fun detect(
        image: InputImage,
        timestampMs: Long,
        onSuccess: (List<DetectedFace>) -> Unit,
        onFailure: (Throwable) -> Unit,
        onComplete: () -> Unit
    ) {
        detector.process(image)
            .addOnSuccessListener(callbackExecutor) { faces ->
                val detectedFaces = faces.map { face ->
                    DetectedFace(
                        boundingBox = FaceBoundingBox(
                            left = face.boundingBox.left,
                            top = face.boundingBox.top,
                            right = face.boundingBox.right,
                            bottom = face.boundingBox.bottom
                        ),
                        trackingId = face.trackingId,
                        headEulerAngleY = face.headEulerAngleY,
                        headEulerAngleZ = face.headEulerAngleZ,
                        smilingProbability = face.smilingProbability,
                        timestampMs = timestampMs
                    )
                }
                onSuccess(detectedFaces)
            }
            .addOnFailureListener(callbackExecutor) { error ->
                onFailure(error)
            }
            .addOnCompleteListener(callbackExecutor) {
                onComplete()
            }
    }

    fun close() {
        runCatching {
            detector.close()
        }.onFailure { error ->
            Log.w(TAG, "Failed to close ML Kit face detector safely.", error)
        }
    }

    companion object {
        private const val TAG = "PerceptionFaceDetector"
    }
}
