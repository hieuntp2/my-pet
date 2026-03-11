package com.aipet.brain.perception.vision.objectdetection.model

data class ObjectDetectionResult(
    val timestampMs: Long,
    val detections: List<DetectedObject>
)

data class DetectedObject(
    val label: String,
    val confidence: Float,
    val boundingBox: ObjectBoundingBox? = null
)

data class ObjectBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
