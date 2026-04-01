package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.UserHabitProfile

/**
 * Snapshot of all evolution system state at a point in time.
 * Consumed by BehaviorEngine, UI debug screens, and greeting logic.
 */
data class EvolutionContext(
    val bond: BondStateV2,
    val habitProfile: UserHabitProfile,
    val dayPhase: DayPhase,
    val reunionType: ReunionType,
    val expectationState: ExpectedReturnWindowResolver.ExpectationState,
    val recentEpisodes: List<MemoryEpisode>,
    val lifecycleModifiers: LifecycleBaselineModifiers,
    val personalityProfile: String
)
