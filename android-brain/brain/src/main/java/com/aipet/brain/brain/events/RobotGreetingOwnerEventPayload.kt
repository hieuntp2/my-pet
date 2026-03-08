package com.aipet.brain.brain.events

data class RobotGreetingOwnerEventPayload(
    val personId: String,
    val seenAtMs: Long,
    val message: String
) {
    fun toJson(): String {
        return buildString(capacity = 112) {
            append("{")
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"seenAtMs\":").append(seenAtMs).append(",")
            append("\"message\":\"").append(message.toJsonEscaped()).append("\"")
            append("}")
        }
    }
}
