package com.aipet.brain.brain.attention

import com.aipet.brain.brain.attention.AttentionTargetEvaluator.FocusCandidate
import com.aipet.brain.brain.attention.SalienceScorer.ScoredCandidate
import com.aipet.brain.brain.b2.domain.WorkingContext

/**
 * Selects the winning focus target from scored candidates.
 *
 * Rules:
 * 1. High-urgency candidates (SOUND_SOURCE when loud, TOUCH_SOURCE) can always interrupt.
 * 2. Strong attention modes (SOCIAL_LOCK, PLAY_FOCUS) resist low-value switches.
 * 3. Winner is the highest effective salience that clears the current stickiness threshold.
 * 4. If no winner clears the threshold, keep current target.
 */
class AttentionArbitrator {

    data class ArbitrationResult(
        val winner: FocusTarget,
        val shifted: Boolean,
        val shiftReason: String,
        val debugEntries: List<AttentionCandidateDebugEntry>
    )

    fun arbitrate(
        scored: List<ScoredCandidate>,
        current: AttentionState,
        ctx: WorkingContext
    ): ArbitrationResult {
        val nowMs = ctx.snapshotAtMs
        val currentTarget = current.activeTarget

        val debugEntries = scored.map { s ->
            AttentionCandidateDebugEntry(
                type = s.candidate.target.type,
                id = s.candidate.target.id,
                rawSalience = s.candidate.rawSalience,
                switchCostPenalty = s.switchCostPenalty,
                effectiveSalience = s.effectiveSalience,
                blocked = false
            )
        }

        val best = scored.maxByOrNull { it.effectiveSalience }

        if (best == null || best.effectiveSalience <= 0f) {
            return ArbitrationResult(
                winner = FocusTarget.NONE,
                shifted = currentTarget.isActive(),
                shiftReason = "no_candidates",
                debugEntries = debugEntries
            )
        }

        // Determine if switch is permitted
        val shouldSwitch = when {
            // Always switch if current has no active target
            !currentTarget.isActive() -> true

            // Urgent interrupts override stickiness
            isUrgentInterrupt(best.candidate.target, ctx) -> true

            // Check stickiness threshold: new target must be significantly better
            best.candidate.target.type != currentTarget.type ->
                best.effectiveSalience > (currentTarget.salience * (1f + current.stickiness))

            // Same type — reinforce current
            else -> false
        }

        return if (shouldSwitch && best.candidate.target.type != currentTarget.type) {
            val winner = best.candidate.target.copy(
                acquiredAtMs = nowMs,
                lastReinforcedAtMs = nowMs
            )
            ArbitrationResult(
                winner = winner,
                shifted = true,
                shiftReason = "higher_salience_${best.candidate.target.type.name.lowercase()}",
                debugEntries = debugEntries
            )
        } else {
            // Reinforce current target with updated salience if same type
            val reinforced = if (currentTarget.isActive() && best.candidate.target.type == currentTarget.type) {
                currentTarget.copy(
                    salience = best.effectiveSalience,
                    lastReinforcedAtMs = nowMs
                )
            } else {
                currentTarget
            }
            ArbitrationResult(
                winner = reinforced,
                shifted = false,
                shiftReason = "held_target",
                debugEntries = debugEntries
            )
        }
    }

    private fun isUrgentInterrupt(target: FocusTarget, ctx: WorkingContext): Boolean = when {
        target.type == FocusTargetType.SOUND_SOURCE && ctx.perception.audioContext.loudEventActive -> true
        target.type == FocusTargetType.TOUCH_SOURCE -> true
        target.type == FocusTargetType.USER_VOICE && ctx.perception.voiceContext.commandAddressedToPet -> true
        else -> false
    }
}
