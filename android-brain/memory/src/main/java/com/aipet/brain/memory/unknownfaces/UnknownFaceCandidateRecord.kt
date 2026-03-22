package com.aipet.brain.memory.unknownfaces

data class UnknownFaceCandidateRecord(
    val candidateId: String,
    val status: UnknownFaceCandidateStatus,
    val representativeEmbedding: List<Float>,
    val previewImageBase64: String?,
    val firstSeenAtMs: Long,
    val lastSeenAtMs: Long,
    val seenFrameCount: Int,
    val seenEncounterCount: Int,
    val averageQualityScore: Float,
    val lastPromptAtMs: Long?,
    val suppressedUntilMs: Long?,
    val closestKnownPersonId: String?,
    val closestKnownSimilarity: Float?,
    val lastDecision: UnknownFaceDecision,
    val updatedAtMs: Long
)

enum class UnknownFaceCandidateStatus {
    COLLECTING,
    READY_TO_ASK,
    ASKED,
    TEACHING,
    RESOLVED,
    SUPPRESSED
}

enum class UnknownFaceDecision {
    KNOWN,
    UNCERTAIN,
    UNKNOWN
}
