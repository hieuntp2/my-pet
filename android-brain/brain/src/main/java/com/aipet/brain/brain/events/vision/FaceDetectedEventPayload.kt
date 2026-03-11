package com.aipet.brain.brain.events.vision

data class FaceDetectedEventPayload(
    val frameId: Long,
    val timestampMs: Long,
    val faceCount: Int,
    val boundingBox: FaceBoundingBoxPayload,
    val trackingId: Int?,
    val headEulerAngleY: Float,
    val headEulerAngleZ: Float,
    val smilingProbability: Float?
) {
    fun toJson(): String {
        return buildString(capacity = 256) {
            append("{")
            append("\"frameId\":").append(frameId).append(",")
            append("\"timestampMs\":").append(timestampMs).append(",")
            append("\"faceCount\":").append(faceCount).append(",")
            append("\"boundingBox\":").append(boundingBox.toJson()).append(",")
            append("\"trackingId\":").append(trackingId ?: "null").append(",")
            append("\"headEulerAngleY\":").append(headEulerAngleY).append(",")
            append("\"headEulerAngleZ\":").append(headEulerAngleZ).append(",")
            append("\"smilingProbability\":").append(smilingProbability ?: "null")
            append("}")
        }
    }
}
