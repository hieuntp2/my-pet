package com.aipet.brain.brain.events

data class CandidatePersonReadyForTeachPayload(
    val sessionId: String,
    val sampleCount: Int,
    val stableScore: Float,
    val centroidEmbedding: List<Float>,
    val previewImageBase64: String?,
    val readyAtMs: Long,
    val candidateId: String = sessionId,
    val decision: String = "UNKNOWN",
    val seenFrameCount: Int = sampleCount,
    val seenEncounterCount: Int = 1,
    val averageQualityScore: Float = 0f,
    val lastPromptAtMs: Long? = null,
    val suppressedUntilMs: Long? = null,
    val closestKnownPersonId: String? = null,
    val closestKnownSimilarity: Float? = null,
    val activeCandidateCount: Int = 1
) {
    fun toJson(): String {
        return buildString(capacity = 720 + centroidEmbedding.size * 10) {
            append("{")
            append("\"sessionId\":\"").append(sessionId.toJsonEscaped()).append("\",")
            append("\"candidateId\":\"").append(candidateId.toJsonEscaped()).append("\",")
            append("\"sampleCount\":").append(sampleCount).append(",")
            append("\"stableScore\":").append(stableScore).append(",")
            append("\"decision\":\"").append(decision.toJsonEscaped()).append("\",")
            append("\"seenFrameCount\":").append(seenFrameCount).append(",")
            append("\"seenEncounterCount\":").append(seenEncounterCount).append(",")
            append("\"averageQualityScore\":").append(averageQualityScore).append(",")
            append("\"centroidEmbedding\":[")
            centroidEmbedding.forEachIndexed { index, value ->
                if (index > 0) append(",")
                append(value)
            }
            append("],")
            append("\"previewImageBase64\":")
            if (previewImageBase64 == null) {
                append("null")
            } else {
                append("\"").append(previewImageBase64.toJsonEscaped()).append("\"")
            }
            append(",")
            append("\"readyAtMs\":").append(readyAtMs).append(",")
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
            append("\"activeCandidateCount\":").append(activeCandidateCount)
            append("}")
        }
    }

    companion object {
        private val SESSION_ID_PATTERN = Regex("\"sessionId\"\\s*:\\s*\"([^\"]+)\"")
        private val CANDIDATE_ID_PATTERN = Regex("\"candidateId\"\\s*:\\s*\"([^\"]+)\"")
        private val SAMPLE_COUNT_PATTERN = Regex("\"sampleCount\"\\s*:\\s*(-?\\d+)")
        private val STABLE_SCORE_PATTERN = Regex("\"stableScore\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val DECISION_PATTERN = Regex("\"decision\"\\s*:\\s*\"([^\"]+)\"")
        private val SEEN_FRAME_COUNT_PATTERN = Regex("\"seenFrameCount\"\\s*:\\s*(-?\\d+)")
        private val SEEN_ENCOUNTER_COUNT_PATTERN = Regex("\"seenEncounterCount\"\\s*:\\s*(-?\\d+)")
        private val AVERAGE_QUALITY_PATTERN = Regex("\"averageQualityScore\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)")
        private val CENTROID_PATTERN = Regex("\"centroidEmbedding\"\\s*:\\s*\\[([^\\]]*)\\]")
        private val PREVIEW_PATTERN = Regex("\"previewImageBase64\"\\s*:\\s*(null|\"([^\"]*)\")")
        private val READY_AT_PATTERN = Regex("\"readyAtMs\"\\s*:\\s*(-?\\d+)")
        private val LAST_PROMPT_PATTERN = Regex("\"lastPromptAtMs\"\\s*:\\s*(null|-?\\d+)")
        private val SUPPRESSED_UNTIL_PATTERN = Regex("\"suppressedUntilMs\"\\s*:\\s*(null|-?\\d+)")
        private val CLOSEST_KNOWN_PERSON_PATTERN = Regex("\"closestKnownPersonId\"\\s*:\\s*(null|\"([^\"]*)\")")
        private val CLOSEST_KNOWN_SIM_PATTERN = Regex("\"closestKnownSimilarity\"\\s*:\\s*(null|-?\\d+(?:\\.\\d+)?)")
        private val ACTIVE_CANDIDATE_COUNT_PATTERN = Regex("\"activeCandidateCount\"\\s*:\\s*(-?\\d+)")

        fun fromJson(payloadJson: String): CandidatePersonReadyForTeachPayload? {
            val sessionId = SESSION_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: return null
            val candidateId = CANDIDATE_ID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.ifBlank { null }
                ?: sessionId
            val sampleCount = SAMPLE_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: return null
            val stableScore = STABLE_SCORE_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: return null
            val centroidEmbedding = parseCentroidEmbedding(payloadJson) ?: return null
            val previewImageBase64 = PREVIEW_PATTERN.find(payloadJson)
                ?.let { match ->
                    if (match.groupValues[1] == "null") {
                        null
                    } else {
                        match.groupValues.getOrNull(2)?.ifBlank { null }
                    }
                }
            val readyAtMs = READY_AT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toLongOrNull()
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
                ?: sampleCount
            val seenEncounterCount = SEEN_ENCOUNTER_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 1
            val averageQualityScore = AVERAGE_QUALITY_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toFloatOrNull()
                ?: 0f
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
            val activeCandidateCount = ACTIVE_CANDIDATE_COUNT_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.toIntOrNull()
                ?: 1
            return CandidatePersonReadyForTeachPayload(
                sessionId = sessionId,
                sampleCount = sampleCount,
                stableScore = stableScore,
                centroidEmbedding = centroidEmbedding,
                previewImageBase64 = previewImageBase64,
                readyAtMs = readyAtMs,
                candidateId = candidateId,
                decision = decision,
                seenFrameCount = seenFrameCount,
                seenEncounterCount = seenEncounterCount,
                averageQualityScore = averageQualityScore,
                lastPromptAtMs = lastPromptAtMs,
                suppressedUntilMs = suppressedUntilMs,
                closestKnownPersonId = closestKnownPersonId,
                closestKnownSimilarity = closestKnownSimilarity,
                activeCandidateCount = activeCandidateCount
            )
        }

        private fun parseCentroidEmbedding(payloadJson: String): List<Float>? {
            val rawList = CENTROID_PATTERN.find(payloadJson)
                ?.groupValues
                ?.getOrNull(1)
                ?.trim()
                ?: return null
            if (rawList.isBlank()) {
                return emptyList()
            }
            val values = rawList.split(",")
                .mapNotNull { token ->
                    token.trim().toFloatOrNull()
                }
            return if (values.isEmpty() && rawList.isNotBlank()) {
                null
            } else {
                values
            }
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
