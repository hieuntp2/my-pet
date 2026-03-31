package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.IntentionCandidate
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.b2.domain.WorkingContext

/**
 * Selects the winning intention from a list of scored candidates.
 *
 * Priority rules:
 * 1. Blocked candidates are always excluded.
 * 2. Among unblocked, the highest score wins.
 * 3. If tied, prefer continuity (current plan's intention).
 * 4. If still tied, prefer by stable priority order.
 *
 * Returns the winner and the full sorted candidate list for debug.
 */
class IntentionArbitrator {

    data class ArbitrationResult(
        val winner: PetIntention,
        val winnerScore: Float,
        val winnerReasons: List<String>,
        val sortedCandidates: List<IntentionCandidate>
    )

    fun arbitrate(
        candidates: List<IntentionCandidate>,
        ctx: WorkingContext
    ): ArbitrationResult {
        require(candidates.isNotEmpty()) { "Intention candidates cannot be empty." }

        val eligible = candidates.filter { !it.blocked }
        val sorted = eligible.sortedByDescending { it.score }

        // Choose winner
        val currentIntention = ctx.currentBehavior?.intention
        val winner = selectWinner(sorted, currentIntention)
            ?: candidates.minByOrNull { stabilityOrder(it.intention) }
            ?: candidates.first()

        return ArbitrationResult(
            winner = winner.intention,
            winnerScore = winner.score,
            winnerReasons = winner.reasons,
            sortedCandidates = candidates.sortedByDescending { it.score }
        )
    }

    private fun selectWinner(
        eligible: List<IntentionCandidate>,
        currentIntention: PetIntention?
    ): IntentionCandidate? {
        if (eligible.isEmpty()) return null

        val best = eligible.first()
        // If the top candidate is close in score to the current intention, prefer continuity
        if (currentIntention != null) {
            val currentCandidate = eligible.firstOrNull { it.intention == currentIntention }
            if (currentCandidate != null) {
                val continuityThreshold = 0.08f
                if ((best.score - currentCandidate.score) < continuityThreshold) {
                    return currentCandidate
                }
            }
        }
        return best
    }

    /**
     * Tiebreak ordering — prefer lower numbers when scores are equal.
     * This is only a last-resort; score differences should dominate.
     */
    private fun stabilityOrder(intention: PetIntention): Int = when (intention) {
        PetIntention.RESPOND_TO_USER -> 0
        PetIntention.STARTLE_RECOVER -> 1
        PetIntention.LISTEN -> 2
        PetIntention.PLAY -> 3
        PetIntention.STAY_NEAR -> 4
        PetIntention.SEEK_ATTENTION -> 5
        PetIntention.SEEK_COMFORT -> 6
        PetIntention.INVITE_PLAY -> 7
        PetIntention.INVESTIGATE -> 8
        PetIntention.OBSERVE -> 9
        PetIntention.REQUEST_FOOD -> 10
        PetIntention.CELEBRATE -> 11
        PetIntention.RECOVER -> 12
        PetIntention.SELF_SOOTHE -> 13
        PetIntention.WITHDRAW -> 14
        PetIntention.DOZE -> 15
        PetIntention.REST -> 16
    }
}
