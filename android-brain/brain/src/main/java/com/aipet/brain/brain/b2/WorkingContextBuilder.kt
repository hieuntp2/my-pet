package com.aipet.brain.brain.b2

import com.aipet.brain.brain.attention.AttentionStateRepository
import com.aipet.brain.brain.b2.domain.ActiveBehaviorState
import com.aipet.brain.brain.b2.domain.RecentMemorySummary
import com.aipet.brain.brain.b2.domain.SessionContext
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.fusion.PerceptionFusionRepository
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.personality.PetTrait

/**
 * Assembles a [WorkingContext] snapshot from all available live state.
 * Called at the start of each decision cycle.
 */
class WorkingContextBuilder(
    private val fusionRepository: PerceptionFusionRepository,
    private val attentionStateRepository: AttentionStateRepository,
    private val emotionMomentumEngine: EmotionMomentumEngine,
    private val cooldownTracker: CooldownTracker,
    private val relationshipStateBuilder: RelationshipStateBuilder
) {
    fun build(
        petState: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        session: SessionContext,
        currentBehavior: ActiveBehaviorState?,
        recognizedPersonFamiliarity: Float?,
        recentInteractionCount: Int,
        sessionAbsenceMs: Long,
        recentMemory: RecentMemorySummary,
        nowMs: Long
    ): WorkingContext {
        val relationship = relationshipStateBuilder.build(
            petState = petState,
            recognizedPersonFamiliarity = recognizedPersonFamiliarity,
            recentInteractionCount = recentInteractionCount,
            sessionAbsenceMs = sessionAbsenceMs
        )
        return WorkingContext(
            petState = petState,
            conditions = conditions,
            traits = traits,
            relationship = relationship,
            emotionMomentum = emotionMomentumEngine.current(),
            perception = fusionRepository.getCurrentFusionSnapshot(),
            attention = attentionStateRepository.current(),
            recentMemory = recentMemory,
            currentBehavior = currentBehavior,
            session = session,
            cooldowns = cooldownTracker.current(),
            snapshotAtMs = nowMs
        )
    }
}
