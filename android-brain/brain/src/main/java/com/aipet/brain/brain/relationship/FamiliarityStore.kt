package com.aipet.brain.brain.relationship

interface FamiliarityStore {
    suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): Boolean
}
