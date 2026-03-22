package com.aipet.brain.brain.events

data class UnknownFaceCandidateLifecyclePayload(
    val candidateId: String,
    val status: String,
    val decision: String,
    val seenFrameCount: Int,
    val seenEncounterCount: Int,
    val averageQualityScore: Float,
    val activeCandidateCount: Int,
    val eventAtMs: Long,
    val lastPromptAtMs: Long? = null,
    val suppressedUntilMs: Long? = null,
    val closestKnownPersonId: String? = null,
    val closestKnownSimilarity: Float? = null,
    val encounterId: String? = null,
    val source: String = "background_unknown_face_candidate"
) {
    fun toJson(): String {
        return buildString(capacity = 360) {
            append("{")
            append("\"candidateId\":\"").append(candidateId.toJsonEscaped()).append("\",")
            append("\"status\":\"").append(status.toJsonEscaped()).append("\",")
            append("\"decision\":\"").append(decision.toJsonEscaped()).append("\",")
            append("\"seenFrameCount\":").append(seenFrameCount).append(",")
            append("\"seenEncounterCount\":").append(seenEncounterCount).append(",")
            append("\"averageQualityScore\":").append(averageQualityScore).append(",")
            append("\"activeCandidateCount\":").append(activeCandidateCount).append(",")
            append("\"eventAtMs\":").append(eventAtMs).append(",")
            append("\"lastPromptAtMs\":")
            if (lastPromptAtMs == null) {
                append("null")
            } else {
                append(lastPromptAtMs)
            }
            append(",")
            append("\"suppressedUntilMs\":")
            if (suppressedUntilMs == null) {
                append("null")
            } else {
                append(suppressedUntilMs)
            }
            append(",")
            append("\"closestKnownPersonId\":")
            if (closestKnownPersonId == null) {
                append("null")
            } else {
                append("\"").append(closestKnownPersonId.toJsonEscaped()).append("\"")
            }
            append(",")
            append("\"closestKnownSimilarity\":")
            if (closestKnownSimilarity == null) {
                append("null")
            } else {
                append(closestKnownSimilarity)
            }
            append(",")
            append("\"encounterId\":")
            if (encounterId == null) {
                append("null")
            } else {
                append("\"").append(encounterId.toJsonEscaped()).append("\"")
            }
            append(",")
            append("\"source\":\"").append(source.toJsonEscaped()).append("\"")
            append("}")
        }
    }

    companion object {
        private val CANDIDATE_ID_PATTERN = Regex("\"candidateId\"\\s*:\\s*\"([^\"]+)\"")
        private val STATUS_PATTERN = Regex("\"status\"\\s*:\\s*\"([^\"]+)\"")
        private val DECISION_PATTERN = Regex("\"decision\"\\s*:\\s*\"([^\"]+)\"")
        private val SEEN_FRAME_COUNT_PATTERN = Regex("\"seenFrameCount\"\\s*:\\s*(-?\\d+)")
        private val SEEN_ENCOUNTER_COUNT_PATTERN = Regex("\"seenEncounterCount\"\\s*:\\s*(-?\\d+)")
        private val AVERAGE_QUALITY_PATTERN = Regex("\"averageQualityScore\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val ACTIVE_COUNT_PATTERN = Regex("\"activeCandidateCount\"\\s*:\\s*(-?\\d+)")
        private val EVENT_AT_PATTERN = Regex("\"eventAtMs\"\\s*:\\s*(-?\\d+)")
        private val LAST_PROMPT_PATTERN = Regex("\"lastPromptAtMs\"\\s*:\\s*(null|-?\\d+)")
        private val SUPPRESSED_UNTIL_PATTERN = Regex("\"suppressedUntilMs\"\\s*:\\s*(null|-?\\d+)")
        private val CLOSEST_KNOWN_PERSON_PATTERN = Regex("\"closestKnownPersonId\"\\s*:\\s*(null|\"([^\"]*)\")")
        private val CLOSEST_KNOWN_SIM_PATTERN = Regex("\"closestKnownSimilarity\"\\s*:\\s*(null|-?\\d+(?:\\.\\d+)?)")
        private val ENCOUNTER_ID_PATTERN = Regex("\"encounterId\"\\s*:\\s*(null|\"([^\"]*)\")")
        private val SOURCE_PATTERN = Regex("\"source\"\\s*:\\s*\"([^\"]+)\"")

        fun fromJson(payloadJson: String): UnknownFaceCandidateLifecyclePayload? {
            val candidateId = CANDIDATE_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val status = STATUS_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val decision = DECISION_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: "UNKNOWN"
            val seenFrameCount = SEEN_FRAME_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val seenEncounterCount = SEEN_ENCOUNTER_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val averageQualityScore = AVERAGE_QUALITY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: 0f
            val activeCandidateCount = ACTIVE_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 0
            val eventAtMs = EVENT_AT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
                ?: return null
            val lastPromptAtMs = LAST_PROMPT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toNullableLong()
            val suppressedUntilMs = SUPPRESSED_UNTIL_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toNullableLong()
            val closestKnownPersonId = CLOSEST_KNOWN_PERSON_PATTERN.find(payloadJson)
                ?.let { match ->
                    if (match.groupValues[1] == "null") {
                        null
                    } else {
                        match.groupValues.getOrNull(2)?.ifBlank { null }
                    }
                }
            val closestKnownSimilarity = CLOSEST_KNOWN_SIM_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toNullableFloat()
            val encounterId = ENCOUNTER_ID_PATTERN.find(payloadJson)
                ?.let { match ->
                    if (match.groupValues[1] == "null") {
                        null
                    } else {
                        match.groupValues.getOrNull(2)?.ifBlank { null }
                    }
                }
            val source = SOURCE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: "background_unknown_face_candidate"
            return UnknownFaceCandidateLifecyclePayload(
                candidateId = candidateId,
                status = status,
                decision = decision,
                seenFrameCount = seenFrameCount,
                seenEncounterCount = seenEncounterCount,
                averageQualityScore = averageQualityScore,
                activeCandidateCount = activeCandidateCount,
                eventAtMs = eventAtMs,
                lastPromptAtMs = lastPromptAtMs,
                suppressedUntilMs = suppressedUntilMs,
                closestKnownPersonId = closestKnownPersonId,
                closestKnownSimilarity = closestKnownSimilarity,
                encounterId = encounterId,
                source = source
            )
        }
    }
}

private fun String.toNullableLong(): Long? {
    if (this == "null") {
        return null
    }
    return toLongOrNull()
}

private fun String.toNullableFloat(): Float? {
    if (this == "null") {
        return null
    }
    return toFloatOrNull()
}
