package com.aipet.brain.brain.events

data class PersonRecognizedPayload(
    val personId: String,
    val similarityScore: Float,
    val threshold: Float,
    val evaluatedCandidates: Int,
    val timestamp: Long
) {
    fun toJson(): String {
        return buildString(capacity = 192) {
            append("{")
            append("\"personId\":\"").append(personId.escapeJsonString()).append("\",")
            append("\"similarityScore\":").append(similarityScore).append(",")
            append("\"threshold\":").append(threshold).append(",")
            append("\"evaluatedCandidates\":").append(evaluatedCandidates).append(",")
            append("\"timestamp\":").append(timestamp)
            append("}")
        }
    }

    companion object {
        private val PERSON_ID_PATTERN = Regex("\"personId\"\\s*:\\s*\"([^\"]+)\"")
        private val SIMILARITY_SCORE_PATTERN = Regex("\"similarityScore\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val THRESHOLD_PATTERN = Regex("\"threshold\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val EVALUATED_CANDIDATES_PATTERN = Regex("\"evaluatedCandidates\"\\s*:\\s*(-?\\d+)")
        private val TIMESTAMP_PATTERN = Regex("\"timestamp\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): PersonRecognizedPayload? {
            val personId = PERSON_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val similarityScore = SIMILARITY_SCORE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val threshold = THRESHOLD_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val evaluatedCandidates = EVALUATED_CANDIDATES_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val timestamp = TIMESTAMP_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            return PersonRecognizedPayload(
                personId = personId,
                similarityScore = similarityScore,
                threshold = threshold,
                evaluatedCandidates = evaluatedCandidates,
                timestamp = timestamp
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
