package com.aipet.brain.brain.events

data class PersonTeachAutoCapturePayload(
    val candidateId: String,
    val personId: String,
    val profileId: String,
    val windowStartAtMs: Long,
    val windowEndAtMs: Long,
    val targetSampleCount: Int,
    val capturedSampleCount: Int,
    val status: String,
    val completedAtMs: Long? = null,
    val completionReason: String? = null
) {
    fun toJson(): String {
        return buildString(capacity = 320) {
            append("{")
            append("\"candidateId\":\"").append(candidateId.toJsonEscaped()).append("\",")
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"profileId\":\"").append(profileId.toJsonEscaped()).append("\",")
            append("\"windowStartAtMs\":").append(windowStartAtMs).append(",")
            append("\"windowEndAtMs\":").append(windowEndAtMs).append(",")
            append("\"targetSampleCount\":").append(targetSampleCount).append(",")
            append("\"capturedSampleCount\":").append(capturedSampleCount).append(",")
            append("\"status\":\"").append(status.toJsonEscaped()).append("\",")
            append("\"completedAtMs\":")
            if (completedAtMs == null) {
                append("null")
            } else {
                append(completedAtMs)
            }
            append(",")
            append("\"completionReason\":")
            if (completionReason == null) {
                append("null")
            } else {
                append("\"").append(completionReason.toJsonEscaped()).append("\"")
            }
            append("}")
        }
    }
}
