package com.aipet.brain.brain.b2.domain

import com.aipet.brain.brain.attention.AttentionState
import com.aipet.brain.brain.evolution.EvolutionContext
import com.aipet.brain.brain.fusion.PerceptionFusionSnapshot
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.personality.PetTrait

/**
 * The complete decision-making snapshot consumed by Behavior Engine v2.
 * All intention scoring and plan selection reads from this stable snapshot.
 *
 * This prevents scattered reads from multiple live flows during a single
 * decision cycle and ensures consistency.
 */
data class WorkingContext(
    val petState: PetState,
    val conditions: Set<PetCondition>,
    val traits: PetTrait?,
    val relationship: RelationshipState,
    val emotionMomentum: EmotionMomentum,
    val perception: PerceptionFusionSnapshot,
    val attention: AttentionState,
    val recentMemory: RecentMemorySummary,
    val currentBehavior: ActiveBehaviorState?,
    val session: SessionContext,
    val cooldowns: CooldownState,
    val snapshotAtMs: Long,
    /** Evolution system context supplied from EvolutionCoordinator. Null until system is warm. */
    val evolutionContext: EvolutionContext? = null
)

/**
 * Describes the currently executing behavior plan (if any).
 */
data class ActiveBehaviorState(
    val planId: String,
    val intention: PetIntention,
    val startedAtMs: Long,
    val expectedDurationMs: Long,
    val interruptPriority: Int
) {
    fun isExpired(nowMs: Long): Boolean =
        expectedDurationMs > 0L && (nowMs - startedAtMs) > expectedDurationMs
}
