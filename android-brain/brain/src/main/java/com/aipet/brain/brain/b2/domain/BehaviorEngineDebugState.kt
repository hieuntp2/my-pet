package com.aipet.brain.brain.b2.domain

/**
 * Debug state snapshot for the full behavior engine v2, exposed to the debug panel.
 */
data class BehaviorEngineDebugState(
    val activeIntention: PetIntention,
    val activePlanId: String,
    val activePlanLabel: String,
    val topCandidates: List<IntentionCandidateDebugEntry>,
    val recentMemory: RecentMemorySummary,
    val emotionMomentum: EmotionMomentum,
    val relationshipState: RelationshipState,
    val cooldownState: CooldownState,
    val sessionContext: SessionContext?,
    val lastScoringCycleMs: Long
)

data class IntentionCandidateDebugEntry(
    val intention: PetIntention,
    val score: Float,
    val reasons: List<String>,
    val blocked: Boolean,
    val blockReason: String?,
    val isWinner: Boolean
)
