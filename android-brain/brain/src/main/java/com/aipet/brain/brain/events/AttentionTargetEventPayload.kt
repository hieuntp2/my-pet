package com.aipet.brain.brain.events

data class AttentionTargetEventPayload(
    val targetType: String,
    val targetId: String? = null,
    val previousTargetType: String? = null,
    val previousTargetId: String? = null,
    val mode: String,
    val intensity: Float,
    val shiftReason: String,
    val timestampMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 240) {
            append("{")
            append("\"targetType\":\"").append(targetType.toJsonEscaped()).append("\",")
            if (!targetId.isNullOrBlank()) {
                append("\"targetId\":\"").append(targetId.toJsonEscaped()).append("\",")
            }
            if (!previousTargetType.isNullOrBlank()) {
                append("\"previousTargetType\":\"").append(previousTargetType.toJsonEscaped()).append("\",")
            }
            if (!previousTargetId.isNullOrBlank()) {
                append("\"previousTargetId\":\"").append(previousTargetId.toJsonEscaped()).append("\",")
            }
            append("\"mode\":\"").append(mode.toJsonEscaped()).append("\",")
            append("\"intensity\":").append(intensity).append(",")
            append("\"shiftReason\":\"").append(shiftReason.toJsonEscaped()).append("\",")
            append("\"timestampMs\":").append(timestampMs)
            append("}")
        }
    }
}
