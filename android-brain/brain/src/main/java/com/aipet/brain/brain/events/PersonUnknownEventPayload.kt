package com.aipet.brain.brain.events

data class PersonUnknownEventPayload(
    val seenAtMs: Long,
    val source: String
) {
    fun toJson(): String {
        return buildString(capacity = 72) {
            append("{")
            append("\"seenAtMs\":").append(seenAtMs).append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\"")
            append("}")
        }
    }
}
