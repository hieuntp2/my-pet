package com.aipet.brain.brain.events.vision

data class FaceBoundingBoxPayload(
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int
) {
    fun toJson(): String {
        return buildString(capacity = 64) {
            append("{")
            append("\"left\":").append(left).append(",")
            append("\"top\":").append(top).append(",")
            append("\"right\":").append(right).append(",")
            append("\"bottom\":").append(bottom)
            append("}")
        }
    }
}
