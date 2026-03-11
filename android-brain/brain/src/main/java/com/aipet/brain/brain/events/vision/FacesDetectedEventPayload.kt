package com.aipet.brain.brain.events.vision

data class FacesDetectedEventPayload(
    val frameId: Long,
    val timestampMs: Long,
    val faceCount: Int,
    val boundingBoxes: List<FaceBoundingBoxPayload>
) {
    fun toJson(): String {
        return buildString(capacity = 192) {
            append("{")
            append("\"frameId\":").append(frameId).append(",")
            append("\"timestampMs\":").append(timestampMs).append(",")
            append("\"faceCount\":").append(faceCount).append(",")
            append("\"boundingBoxes\":[")
            boundingBoxes.forEachIndexed { index, box ->
                if (index > 0) {
                    append(",")
                }
                append(box.toJson())
            }
            append("]")
            append("}")
        }
    }
}
