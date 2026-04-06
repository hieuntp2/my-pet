package com.aipet.brain.brain.events

data class PetIdleActivityPayload(
    val emittedAtMs: Long,
    val visualIntent: String,
    val sourceIntention: String?,
    val attentionMode: String?,
    val attentionIntensity: Float,
    val reason: String
) {
    fun toJson(): String {
        return buildString(capacity = 256) {
            append("{")
            append("\"emittedAtMs\":").append(emittedAtMs).append(",")
            append("\"visualIntent\":\"").append(visualIntent.toJsonEscaped()).append("\",")
            append("\"sourceIntention\":")
            if (sourceIntention != null) {
                append("\"").append(sourceIntention.toJsonEscaped()).append("\"")
            } else {
                append("null")
            }
            append(",")
            append("\"attentionMode\":")
            if (attentionMode != null) {
                append("\"").append(attentionMode.toJsonEscaped()).append("\"")
            } else {
                append("null")
            }
            append(",")
            append("\"attentionIntensity\":")
                .append(String.format(java.util.Locale.US, "%.3f", attentionIntensity.coerceIn(0f, 1f)))
                .append(",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\"")
            append("}")
        }
    }
}
