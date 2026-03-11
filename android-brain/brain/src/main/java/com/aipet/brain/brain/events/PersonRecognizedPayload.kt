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
            append("\"personId\":\"").append(personId.toJsonEscaped()).append("\",")
            append("\"similarityScore\":").append(similarityScore).append(",")
            append("\"threshold\":").append(threshold).append(",")
            append("\"evaluatedCandidates\":").append(evaluatedCandidates).append(",")
            append("\"timestamp\":").append(timestamp)
            append("}")
        }
    }
}
