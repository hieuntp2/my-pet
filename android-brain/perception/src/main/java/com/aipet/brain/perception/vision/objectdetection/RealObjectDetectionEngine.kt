package com.aipet.brain.perception.vision.objectdetection

import android.content.res.AssetManager
import android.graphics.Bitmap
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionModelConfig
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult

/**
 * Production object detector backed by the on-device TFLite implementation.
 * Kept as a stable entry point for camera/perception wiring in subsequent tasks.
 */
class RealObjectDetectionEngine(
    assetManager: AssetManager,
    modelConfig: ObjectDetectionModelConfig = ObjectDetectionModelConfig()
) : ObjectDetectionEngine {

    private val delegate = TfliteObjectDetectionEngine(
        assetManager = assetManager,
        modelConfig = modelConfig
    )

    override val modelName: String?
        get() = delegate.modelName

    override fun detectObjects(
        frameBitmap: Bitmap,
        timestampMs: Long,
        rotationDegrees: Int
    ): Result<ObjectDetectionResult> {
        return delegate.detectObjects(
            frameBitmap = frameBitmap,
            timestampMs = timestampMs,
            rotationDegrees = rotationDegrees
        )
    }

    override fun close() {
        delegate.close()
    }
}
