package com.aipet.brain.brain.b2.domain

/**
 * A scored candidate intention from the arbitration pass.
 *
 * @param intention    The candidate intention.
 * @param score        Final computed score (higher = more likely to win).
 * @param reasons      Human-readable scoring trace for debug and tuning.
 * @param blocked      Whether this candidate is gated out regardless of score.
 * @param blockReason  Why it was blocked (non-null only when [blocked] is true).
 */
data class IntentionCandidate(
    val intention: PetIntention,
    val score: Float,
    val reasons: List<String>,
    val blocked: Boolean = false,
    val blockReason: String? = null
) {
    /** Returns a copy with an additional scoring reason appended. */
    fun withReason(reason: String): IntentionCandidate =
        copy(reasons = reasons + reason)

    /** Returns a copy with score adjusted by [delta]. */
    fun withScoreDelta(delta: Float, reason: String): IntentionCandidate =
        copy(score = score + delta, reasons = reasons + reason)

    /** Returns a copy marked as blocked. */
    fun blocked(reason: String): IntentionCandidate =
        copy(blocked = true, blockReason = reason)
}
