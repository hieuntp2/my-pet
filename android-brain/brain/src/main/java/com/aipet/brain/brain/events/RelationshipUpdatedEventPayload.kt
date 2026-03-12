package com.aipet.brain.brain.events

data class RelationshipUpdatedEventPayload(
    val personId: String,
    val familiarityScore: Float,
    val updatedAtMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 96) {
            append("{")
            append("\"personId\":\"").append(personId.escapeJsonString()).append("\",")
            append("\"familiarityScore\":").append(familiarityScore).append(",")
            append("\"updatedAtMs\":").append(updatedAtMs)
            append("}")
        }
    }

    companion object {
        private val PERSON_ID_PATTERN = Regex("\"personId\"\\s*:\\s*\"([^\"]+)\"")
        private val FAMILIARITY_SCORE_PATTERN = Regex("\"familiarityScore\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val UPDATED_AT_MS_PATTERN = Regex("\"updatedAtMs\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): RelationshipUpdatedEventPayload? {
            val personId = PERSON_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val familiarityScore = FAMILIARITY_SCORE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val updatedAtMs = UPDATED_AT_MS_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            return RelationshipUpdatedEventPayload(
                personId = personId,
                familiarityScore = familiarityScore,
                updatedAtMs = updatedAtMs
            )
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
