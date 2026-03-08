package com.aipet.brain.brain.events

data class OwnerSeenEventPayload(
    val personId: String,
    val seenAtMs: Long,
    val seenCount: Int
) {
    fun toJson(): String {
        return buildString(capacity = 96) {
            append("{")
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"seenAtMs\":").append(seenAtMs).append(",")
            append("\"seenCount\":").append(seenCount)
            append("}")
        }
    }
}
