package com.aipet.brain.perception.vision.objectdetection

import android.graphics.Bitmap
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult

interface ObjectDetectionEngine : AutoCloseable {
    fun detectObjects(
        frameBitmap: Bitmap,
        timestampMs: Long,
        rotationDegrees: Int = 0
    ): Result<ObjectDetectionResult>
}
