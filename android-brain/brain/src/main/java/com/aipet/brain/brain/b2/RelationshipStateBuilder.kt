package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.RelationshipState
import com.aipet.brain.brain.pet.PetState

/**
 * Builds a [RelationshipState] from the available pet and familiarity context.
 * This is a pure transformation — no side effects.
 */
class RelationshipStateBuilder {

    /**
     * Build from current [PetState] and optional familiarity score from person recognition.
     *
     * @param petState                 Current pet state (bond is the primary input).
     * @param recognizedPersonFamiliarity  0–1 familiarity score for the recognized person, or null.
     * @param recentInteractionCount   How many interactions in the recent session window.
     * @param sessionAbsenceMs         How long since the user was last actively engaged.
     */
    fun build(
        petState: PetState,
        recognizedPersonFamiliarity: Float?,
        recentInteractionCount: Int,
        sessionAbsenceMs: Long
    ): RelationshipState {
        val bondNormalized = petState.bond / 100f
        val socialNormalized = petState.social / 100f

        val familiarity = recognizedPersonFamiliarity?.coerceIn(0f, 1f)
            ?: (bondNormalized * 0.8f + socialNormalized * 0.2f)

        val trust = (bondNormalized * 0.7f + familiarity * 0.3f).coerceIn(0f, 1f)

        val recentWarmth = when {
            recentInteractionCount >= 5 -> 0.8f
            recentInteractionCount >= 2 -> 0.5f
            recentInteractionCount >= 1 -> 0.3f
            else -> 0f
        }.coerceIn(0f, 1f)

        val recentNeglect = when {
            sessionAbsenceMs > 86_400_000L -> 0.8f  // >1 day
            sessionAbsenceMs > 3_600_000L -> 0.5f   // >1 hour
            sessionAbsenceMs > 900_000L -> 0.2f     // >15 min
            else -> 0f
        }.coerceIn(0f, 1f)

        val responsivenessExpectation = trust * 0.6f + bondNormalized * 0.4f

        return RelationshipState(
            familiarity = familiarity,
            trust = trust,
            recentWarmth = recentWarmth,
            recentNeglect = recentNeglect,
            responsivenessExpectation = responsivenessExpectation,
            bondScore = petState.bond,
            updatedAtMs = petState.lastUpdatedAt
        )
    }
}
