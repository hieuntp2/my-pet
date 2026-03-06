package com.aipet.brain.brain.events

data class CameraFrameReceivedPayload(
    val width: Int,
    val height: Int,
    val analyzedAtMs: Long,
    val rotationDegrees: Int,
    val processingDurationMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 128) {
            append("{")
            append("\"width\":").append(width).append(",")
            append("\"height\":").append(height).append(",")
            append("\"analyzedAtMs\":").append(analyzedAtMs).append(",")
            append("\"rotationDegrees\":").append(rotationDegrees).append(",")
            append("\"processingDurationMs\":").append(processingDurationMs)
            append("}")
        }
    }
}
