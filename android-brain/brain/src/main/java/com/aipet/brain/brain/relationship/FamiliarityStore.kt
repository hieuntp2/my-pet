package com.aipet.brain.brain.relationship

data class FamiliarityIncreaseResult(
    val personId: String,
    val previousFamiliarityScore: Float,
    val updatedFamiliarityScore: Float,
    val updatedAtMs: Long
)

interface FamiliarityStore {
    suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): FamiliarityIncreaseResult?
}
