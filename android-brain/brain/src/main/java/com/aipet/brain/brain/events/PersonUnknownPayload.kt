package com.aipet.brain.brain.events

data class PersonUnknownPayload(
    val bestScore: Float,
    val threshold: Float,
    val evaluatedCandidates: Int,
    val timestamp: Long
) {
    fun toJson(): String {
        return buildString(capacity = 168) {
            append("{")
            append("\"bestScore\":").append(bestScore).append(",")
            append("\"threshold\":").append(threshold).append(",")
            append("\"evaluatedCandidates\":").append(evaluatedCandidates).append(",")
            append("\"timestamp\":").append(timestamp)
            append("}")
        }
    }
}
