package com.aipet.brain.brain.events

data class UserInteractedPetEventPayload(
    val interactedAtMs: Long,
    val source: String
) {
    fun toJson(): String {
        return buildString(capacity = 80) {
            append("{")
            append("\"interactedAtMs\":").append(interactedAtMs).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\"")
            append("}")
        }
    }
}
