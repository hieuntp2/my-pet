package com.aipet.brain.perception.vision.model

data class FaceDetectionResult(
    val frameId: Long,
    val timestampMs: Long,
    val frameWidth: Int,
    val frameHeight: Int,
    val rotationDegrees: Int,
    val faces: List<DetectedFace>
)
