package com.aipet.brain.perception.vision.model

data class DetectedFace(
    val boundingBox: FaceBoundingBox,
    val trackingId: Int?,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val smilingProbability: Float?,
    val timestampMs: Long
)

data class FaceBoundingBox(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
)
