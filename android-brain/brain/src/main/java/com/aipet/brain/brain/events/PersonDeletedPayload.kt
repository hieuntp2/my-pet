package com.aipet.brain.brain.events

data class PersonDeletedPayload(
    val personId: String,
    val displayName: String,
    val deletedAtMs: Long,
    val profileCount: Int,
    val embeddingCount: Int
) {
    fun toJson(): String {
        return buildString(capacity = 192) {
            append("{")
            append("\"personId\":\"").append(personId.escapeJsonString()).append("\",")
            append("\"displayName\":\"").append(displayName.escapeJsonString()).append("\",")
            append("\"deletedAtMs\":").append(deletedAtMs).append(",")
            append("\"profileCount\":").append(profileCount).append(",")
            append("\"embeddingCount\":").append(embeddingCount)
            append("}")
        }
    }
}

private fun String.escapeJsonString(): String {
    return buildString(length + 8) {
        for (char in this@escapeJsonString) {
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> append(char)
            }
        }
    }
}
