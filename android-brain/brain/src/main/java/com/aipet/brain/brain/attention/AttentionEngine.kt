package com.aipet.brain.brain.attention

import android.util.Log
import com.aipet.brain.brain.attention.AttentionTargetEvaluator.FocusCandidate
import com.aipet.brain.brain.b2.domain.WorkingContext
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Main attention system engine.
 *
 * On each fast-to-medium cycle:
 * 1. Evaluate candidate focus targets from working context.
 * 2. Score them with salience + switch cost.
 * 3. Arbitrate winner with attention stickiness rules.
 * 4. Update attention mode.
 * 5. Update fatigue.
 * 6. Write new [AttentionState] to the repository.
 *
 * The [WorkingContext] must be built before calling this engine;
 * it provides the fused perception + state + relationship snapshot.
 */
class AttentionEngine(
    private val repository: AttentionStateRepository,
    private val evaluator: AttentionTargetEvaluator,
    private val scorer: SalienceScorer,
    private val arbitrator: AttentionArbitrator,
    private val modeResolver: AttentionModeResolver,
    private val fatigueTracker: AttentionFatigueTracker,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val lock = Mutex()

    private var lastShiftReason: String = "initial"

    private val _debugState = kotlinx.coroutines.flow.MutableStateFlow<AttentionDebugState?>(null)
    val debugState: StateFlow<AttentionDebugState?> = _debugState

    /** Run one attention update cycle from the given [WorkingContext]. */
    suspend fun update(ctx: WorkingContext) {
        lock.withLock {
            val nowMs = ctx.snapshotAtMs
            val current = repository.current()

            // Step 1: Evaluate candidates
            val candidates: List<FocusCandidate> = evaluator.evaluate(ctx)

            // Step 2: Score
            val scored = scorer.score(candidates, current.activeTarget, ctx)

            // Step 3: Arbitrate
            val result = arbitrator.arbitrate(scored, current, ctx)

            // Step 4: Resolve mode
            val newMode = modeResolver.resolve(ctx, current.mode)

            // Step 5: Fatigue
            val newFatigue = fatigueTracker.computeFatigue(current, result.shifted, ctx)

            // Step 6: Compute stickiness from mode
            val newStickiness = stickinessForMode(newMode)

            // Step 7: Write state
            val shiftAtMs = if (result.shifted) nowMs else current.lastShiftAtMs
            if (result.shifted) {
                lastShiftReason = result.shiftReason
                Log.d(TAG, "Attention shift: ${current.activeTarget.type.name} → " +
                    "${result.winner.type.name} reason=${result.shiftReason}")
            }

            val newState = AttentionState(
                activeTarget = result.winner,
                mode = newMode,
                intensity = result.winner.salience,
                stickiness = newStickiness,
                fatigue = newFatigue,
                availableForInterrupt = newMode != AttentionMode.SOCIAL_LOCK,
                lastShiftAtMs = shiftAtMs,
                updatedAtMs = nowMs
            )
            repository.update(newState)

            // Step 8: Debug state
            _debugState.value = AttentionDebugState(
                currentMode = newMode,
                activeTarget = result.winner,
                intensity = newState.intensity,
                stickiness = newStickiness,
                fatigue = newFatigue,
                holdDurationMs = result.winner.holdDurationMs(nowMs),
                availableForInterrupt = newState.availableForInterrupt,
                topCandidates = result.debugEntries.take(5),
                lastShiftAtMs = shiftAtMs,
                lastShiftReason = lastShiftReason
            )
        }
    }

    fun observeAttentionState(): StateFlow<AttentionState> = repository.observe()

    private fun stickinessForMode(mode: AttentionMode): Float = when (mode) {
        AttentionMode.SOCIAL_LOCK -> 0.85f
        AttentionMode.PLAY_FOCUS -> 0.75f
        AttentionMode.DOZING -> 0.70f
        AttentionMode.WITHDRAWN -> 0.60f
        AttentionMode.LISTENING -> 0.50f
        AttentionMode.ALERT -> 0.10f  // low: alert should redirect quickly
        AttentionMode.PASSIVE_COMPANION -> 0.40f
        AttentionMode.CURIOUS_INSPECTION -> 0.30f
        AttentionMode.IDLE_SCANNING -> 0.15f
    }

    companion object {
        private const val TAG = "AttentionEngine"
    }
}
