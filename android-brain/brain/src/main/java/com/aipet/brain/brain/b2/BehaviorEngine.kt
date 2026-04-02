package com.aipet.brain.brain.b2

import android.util.Log
import com.aipet.brain.brain.attention.AttentionStateRepository
import com.aipet.brain.brain.b2.domain.ActiveBehaviorState
import com.aipet.brain.brain.b2.domain.BehaviorEngineDebugState
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.domain.IntentionCandidateDebugEntry
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.b2.domain.RecentMemorySummary
import com.aipet.brain.brain.b2.domain.RelationshipState
import com.aipet.brain.brain.b2.domain.SessionContext
import com.aipet.brain.brain.b2.domain.WorkingContext
import com.aipet.brain.brain.evolution.EvolutionContext
import com.aipet.brain.brain.fusion.PerceptionFusionRepository
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.personality.PetTrait
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Behavior Engine v2 — the central decision-making system.
 *
 * Converts all available information (state, perception, attention, memory, session)
 * into a coherent [BehaviorPlan] that execution layers can drive.
 *
 * Architecture:
 *   WorkingContext → IntentionScorer → IntentionArbitrator → BehaviorPlanner → BehaviorPlan
 *
 * The engine is called on the medium loop (500ms–2s) from outside.
 * Callers read the active plan via [observeActivePlan].
 */
class BehaviorEngine(
    private val fusionRepository: PerceptionFusionRepository,
    private val attentionStateRepository: AttentionStateRepository,
    val emotionMomentumEngine: EmotionMomentumEngine,
    val cooldownTracker: CooldownTracker,
    private val relationshipStateBuilder: RelationshipStateBuilder,
    private val scorer: IntentionScorer,
    private val arbitrator: IntentionArbitrator,
    private val planner: BehaviorPlanner,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val lock = Mutex()

    private val _activePlan = MutableStateFlow<BehaviorPlan>(BehaviorPlan.IDLE_DEFAULT)
    val activePlan: StateFlow<BehaviorPlan> = _activePlan.asStateFlow()

    private val _debugState = MutableStateFlow<BehaviorEngineDebugState?>(null)
    val debugState: StateFlow<BehaviorEngineDebugState?> = _debugState.asStateFlow()

    @Volatile
    private var currentBehaviorState: ActiveBehaviorState? = null

    @Volatile
    private var sessionContext: SessionContext? = null

    /** Set/update the session context at app open or session boundary. */
    fun setSessionContext(ctx: SessionContext) {
        sessionContext = ctx
    }

    /**
     * Run a full decision cycle. Call this on the medium loop.
     *
     * @param petState     Current persisted pet state.
     * @param conditions   Conditions derived from petState.
     * @param traits       Personality traits.
     * @param recognizedPersonFamiliarity  Familiarity score for recognized person (0–1 or null).
     * @param recentInteractionCount  Number of recent interactions in session.
     * @param sessionAbsenceMs  How long since last active engagement.
     * @param recentMemory  Short-term perception history summary.
     */
    suspend fun runDecisionCycle(
        petState: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        recognizedPersonFamiliarity: Float?,
        recentInteractionCount: Int,
        sessionAbsenceMs: Long,
        recentMemory: RecentMemorySummary,
        evolutionContext: EvolutionContext? = null
    ): BehaviorPlan {
        return lock.withLock {
            val nowMs = nowProvider()
            val perception = fusionRepository.getCurrentFusionSnapshot()
            val attention = attentionStateRepository.current()
            val emotion = emotionMomentumEngine.current()
            val cooldowns = cooldownTracker.current()
            val relationship = relationshipStateBuilder.build(
                petState = petState,
                recognizedPersonFamiliarity = recognizedPersonFamiliarity,
                recentInteractionCount = recentInteractionCount,
                sessionAbsenceMs = sessionAbsenceMs
            )
            val session = sessionContext ?: defaultSession(nowMs)
            val current = currentBehaviorState

            val ctx = WorkingContext(
                petState = petState,
                conditions = conditions,
                traits = traits,
                relationship = relationship,
                emotionMomentum = emotion,
                perception = perception,
                attention = attention,
                recentMemory = recentMemory,
                currentBehavior = current,
                session = session,
                cooldowns = cooldowns,
                snapshotAtMs = nowMs,
                evolutionContext = evolutionContext
            )

            val candidates = scorer.score(ctx)
            val result = arbitrator.arbitrate(candidates, ctx)
            val plan = planner.plan(result.winner, ctx)

            // Apply plan after-effects immediately
            try {
                cooldownTracker.applyPlanAfterEffects(plan, nowMs)
                emotionMomentumEngine.applyPlanAfterEffects(plan, nowMs)
            } catch (e: Exception) {
                Log.w(TAG, "Failed to apply after-effects for plan ${plan.id}: ${e.message}")
            }

            // Update active behavior state
            currentBehaviorState = ActiveBehaviorState(
                planId = plan.id,
                intention = plan.intention,
                startedAtMs = nowMs,
                expectedDurationMs = plan.expectedDurationMs,
                interruptPriority = plan.interruptPriority
            )

            _activePlan.value = plan

            // Update debug state
            _debugState.value = BehaviorEngineDebugState(
                activeIntention = result.winner,
                activePlanId = plan.id,
                activePlanLabel = plan.debugLabel,
                topCandidates = result.sortedCandidates.take(5).map { c ->
                    IntentionCandidateDebugEntry(
                        intention = c.intention,
                        score = c.score,
                        reasons = c.reasons,
                        blocked = c.blocked,
                        blockReason = c.blockReason,
                        isWinner = c.intention == result.winner
                    )
                },
                emotionMomentum = emotion,
                relationshipState = relationship,
                cooldownState = cooldowns,
                sessionContext = session,
                lastScoringCycleMs = nowMs
            )

            Log.d(TAG, "Decision cycle: winner=${result.winner.name} plan=${plan.debugLabel} score=%.2f".format(result.winnerScore))

            plan
        }
    }

    /** Observe the active plan as a StateFlow for execution layers. */
    fun observeActivePlan(): StateFlow<BehaviorPlan> = activePlan

    /** Returns the current session context, or creates a default one if not yet set. */
    fun getSessionContext(nowMs: Long): SessionContext = sessionContext ?: defaultSession(nowMs)

    private fun defaultSession(nowMs: Long): SessionContext {
        val calendar = java.util.Calendar.getInstance()
        return SessionContext(
            sessionStartMs = nowMs,
            sessionAgeMs = 0L,
            hourOfDay = calendar.get(java.util.Calendar.HOUR_OF_DAY),
            isFirstGreetToday = false,
            sessionInteractionCount = 0,
            msSinceLastDirectInteraction = 60_000L,
            isReturningAfterLongAbsence = false,
            nowMs = nowMs
        )
    }

    companion object {
        private const val TAG = "BehaviorEngineV2"
    }
}
