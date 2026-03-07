package com.aipet.brain.memory.profiles

data class FaceProfileRecord(
    val profileId: String,
    val status: FaceProfileStatus,
    val label: String?,
    val note: String?,
    val linkedPersonId: String?,
    val createdAtMs: Long,
    val updatedAtMs: Long
) {
    val isResolved: Boolean
        get() = !linkedPersonId.isNullOrBlank()
}

enum class FaceProfileStatus {
    CANDIDATE
}

data class FaceProfileObservationLinkRecord(
    val profileId: String,
    val observationId: String,
    val linkedAtMs: Long
)

data class FaceProfileEmbeddingRecord(
    val embeddingId: String,
    val profileId: String,
    val createdAtMs: Long,
    val vectorDim: Int,
    val values: List<Float>,
    val metadata: String?
)
