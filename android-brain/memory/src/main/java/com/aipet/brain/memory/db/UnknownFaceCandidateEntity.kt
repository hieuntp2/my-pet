package com.aipet.brain.memory.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "unknown_face_candidates",
    indices = [
        Index(value = ["status"]),
        Index(value = ["last_seen_at_ms"]),
        Index(value = ["suppressed_until_ms"]),
        Index(value = ["updated_at_ms"])
    ]
)
data class UnknownFaceCandidateEntity(
    @PrimaryKey
    @ColumnInfo(name = "candidate_id")
    val candidateId: String,
    @ColumnInfo(name = "status")
    val status: String,
    @ColumnInfo(name = "representative_embedding_blob")
    val representativeEmbeddingBlob: ByteArray,
    @ColumnInfo(name = "embedding_dim")
    val embeddingDim: Int,
    @ColumnInfo(name = "preview_image_base64")
    val previewImageBase64: String?,
    @ColumnInfo(name = "first_seen_at_ms")
    val firstSeenAtMs: Long,
    @ColumnInfo(name = "last_seen_at_ms")
    val lastSeenAtMs: Long,
    @ColumnInfo(name = "seen_frame_count")
    val seenFrameCount: Int,
    @ColumnInfo(name = "seen_encounter_count")
    val seenEncounterCount: Int,
    @ColumnInfo(name = "average_quality_score")
    val averageQualityScore: Float,
    @ColumnInfo(name = "last_prompt_at_ms")
    val lastPromptAtMs: Long?,
    @ColumnInfo(name = "suppressed_until_ms")
    val suppressedUntilMs: Long?,
    @ColumnInfo(name = "closest_known_person_id")
    val closestKnownPersonId: String?,
    @ColumnInfo(name = "closest_known_similarity")
    val closestKnownSimilarity: Float?,
    @ColumnInfo(name = "last_decision")
    val lastDecision: String,
    @ColumnInfo(name = "updated_at_ms")
    val updatedAtMs: Long
)
