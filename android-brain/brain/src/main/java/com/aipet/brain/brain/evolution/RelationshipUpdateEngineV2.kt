package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.MemoryEpisode

/**
 * Updates the multi-dimensional BondStateV2 based on session outcomes and episode history.
 *
 * Each dimension has independent update logic:
 * - Affection: driven by care quality and positive interaction density
 * - Trust: driven by consistency and reliable reunions
 * - Dependency: driven by regular return rhythm
 * - Stability: driven by absence of oscillation between neglect and care
 */
class RelationshipUpdateEngineV2(
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    data class SessionOutcome(
        val careScoreDelta: Int,
        val interactionCount: Int,
        val hadNeglect: Boolean,
        val reunionType: ReunionType,
        val sessionDurationMs: Long
    )

    fun update(current: BondStateV2, outcome: SessionOutcome, recentEpisodes: List<MemoryEpisode>): BondStateV2 {
        val now = nowProvider()
        var affection = current.affection
        var trust = current.trust
        var dependency = current.dependency
        var stability = current.stability

        // --- Affection update ---
        affection += when {
            outcome.careScoreDelta > 10 -> AFFECTION_STRONG_CARE_GAIN
            outcome.careScoreDelta > 0 -> AFFECTION_MILD_CARE_GAIN
            outcome.hadNeglect -> -AFFECTION_NEGLECT_LOSS
            else -> -AFFECTION_DECAY_PER_SESSION
        }
        if (outcome.interactionCount >= 5) affection += AFFECTION_HIGH_INTERACTION_BONUS
        if (outcome.reunionType == ReunionType.RECOVERY_RETURN) affection += AFFECTION_RECOVERY_BONUS

        // --- Trust update ---
        trust += when (outcome.reunionType) {
            ReunionType.ROUTINE_RETURN -> TRUST_CONSISTENT_GAIN
            ReunionType.LONG_ABSENCE -> -TRUST_LONG_ABSENCE_LOSS
            ReunionType.RECOVERY_RETURN -> -TRUST_RECOVERY_HESITATION
            else -> 0f
        }
        if (outcome.hadNeglect) trust -= TRUST_NEGLECT_COST
        if (outcome.careScoreDelta > 5 && !outcome.hadNeglect) trust += TRUST_RELIABLE_CARE_GAIN

        // --- Dependency update ---
        dependency += when (outcome.reunionType) {
            ReunionType.ROUTINE_RETURN -> DEPENDENCY_ROUTINE_GAIN
            ReunionType.LONG_ABSENCE -> -DEPENDENCY_ABSENCE_DECAY
            else -> 0f
        }
        if (outcome.interactionCount >= 3) dependency += DEPENDENCY_INTERACTION_GAIN

        // --- Stability update ---
        val recentNeglectCount = recentEpisodes.count { it.neglectSignal }
        val recentGoodCount = recentEpisodes.count { it.careScoreDelta > 5 }
        stability += when {
            recentNeglectCount >= 2 && recentGoodCount >= 2 -> -STABILITY_OSCILLATION_PENALTY
            recentNeglectCount == 0 && recentGoodCount >= 2 -> STABILITY_CONSISTENT_GAIN
            outcome.hadNeglect -> -STABILITY_NEGLECT_LOSS
            else -> 0f
        }

        return BondStateV2(
            affection = affection.coerceIn(0f, 1f),
            trust = trust.coerceIn(0f, 1f),
            dependency = dependency.coerceIn(0f, 1f),
            stability = stability.coerceIn(0f, 1f),
            lastUpdatedAtMs = now
        )
    }

    private companion object {
        const val AFFECTION_STRONG_CARE_GAIN = 0.04f
        const val AFFECTION_MILD_CARE_GAIN = 0.015f
        const val AFFECTION_NEGLECT_LOSS = 0.05f
        const val AFFECTION_DECAY_PER_SESSION = 0.005f
        const val AFFECTION_HIGH_INTERACTION_BONUS = 0.02f
        const val AFFECTION_RECOVERY_BONUS = 0.025f

        const val TRUST_CONSISTENT_GAIN = 0.02f
        const val TRUST_LONG_ABSENCE_LOSS = 0.025f
        const val TRUST_RECOVERY_HESITATION = 0.01f
        const val TRUST_NEGLECT_COST = 0.035f
        const val TRUST_RELIABLE_CARE_GAIN = 0.015f

        const val DEPENDENCY_ROUTINE_GAIN = 0.015f
        const val DEPENDENCY_ABSENCE_DECAY = 0.02f
        const val DEPENDENCY_INTERACTION_GAIN = 0.01f

        const val STABILITY_OSCILLATION_PENALTY = 0.03f
        const val STABILITY_CONSISTENT_GAIN = 0.02f
        const val STABILITY_NEGLECT_LOSS = 0.025f
    }
}
