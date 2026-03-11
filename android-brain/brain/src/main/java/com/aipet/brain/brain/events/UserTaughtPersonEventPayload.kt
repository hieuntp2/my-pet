package com.aipet.brain.brain.events

data class UserTaughtPersonEventPayload(
    val personId: String,
    val displayName: String,
    val sampleCount: Int
) {
    fun toJson(): String {
        return buildString(capacity = 160) {
            append("{")
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"displayName\":\"").append(displayName.toJsonEscaped()).append("\",")
            append("\"sampleCount\":").append(sampleCount)
            append("}")
        }
    }
}
