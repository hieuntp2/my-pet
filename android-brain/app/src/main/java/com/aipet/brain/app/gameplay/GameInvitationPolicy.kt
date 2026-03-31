package com.aipet.brain.app.gameplay

import com.aipet.brain.app.ui.home.SparkGamePhase
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.state.BrainState

/**
 * Governs when the pet is eligible to autonomously invite the user to play Spark.
 *
 * Anti-fatigue rules:
 * - Minimum interval between invitations to prevent spamming.
 * - Ignore streak: after MAX_IGNORE_STREAK consecutive ignores, suppression doubles.
 * - Post-play suppression: after a completed game the pet is satisfied; no invite for a while.
 * - Energy suppression: no invitation when pet is sleepy or has low energy.
 * - Game cooldown: no invitation when the game is already in COOLDOWN or any active phase.
 *
 * Eligibility requires at least one of:
 * - PetCondition.PLAYFUL (high energy + social + satisfaction)
 * - PetCondition.LONELY (low social — pet is seeking connection)
 */
class GameInvitationPolicy {

    private var lastInvitationSentAtMs: Long = 0L
    private var lastGameCompletedAtMs: Long = 0L
    private var ignoreStreakCount: Int = 0

    fun isEligible(
        nowMs: Long,
        gamePhase: SparkGamePhase,
        petConditions: Set<PetCondition>,
        brainState: BrainState
    ): Boolean {
        // Game must be available
        if (gamePhase != SparkGamePhase.INACTIVE) return false

        // Pet must not be exhausted or asleep
        if (BrainState.SLEEPY == brainState) return false
        if (PetCondition.SLEEPY in petConditions) return false
        // HUNGRY suppresses invitations — pet needs care first
        if (PetCondition.HUNGRY in petConditions) return false

        // Post-play suppression: pet is satisfied after a game
        if (lastGameCompletedAtMs > 0L &&
            nowMs - lastGameCompletedAtMs < POST_PLAY_SUPPRESSION_MS
        ) return false

        // Anti-spam interval (doubles after ignore streak threshold)
        val requiredInterval = if (ignoreStreakCount >= MAX_IGNORE_STREAK) {
            IGNORE_SUPPRESSION_MS
        } else {
            MIN_INVITATION_INTERVAL_MS
        }
        if (lastInvitationSentAtMs > 0L &&
            nowMs - lastInvitationSentAtMs < requiredInterval
        ) return false

        // Require a playful or lonely drive
        return PetCondition.PLAYFUL in petConditions || PetCondition.LONELY in petConditions
    }

    /** Called when the system emits an invitation to the user. */
    fun recordInvitationSent(nowMs: Long) {
        lastInvitationSentAtMs = nowMs
    }

    /** Called when the game completes (win or lose). Resets ignore streak. */
    fun recordGameCompleted(nowMs: Long) {
        lastGameCompletedAtMs = nowMs
        ignoreStreakCount = 0
        // Also reset invitation timestamp so post-play suppression is anchored here.
        lastInvitationSentAtMs = nowMs
    }

    /** Called when the user ignores a pet-initiated invitation (invite timeout). */
    fun recordInviteIgnored() {
        ignoreStreakCount++
    }

    /** Called when the user accepts the invite — clears the ignore streak. */
    fun resetIgnoreStreak() {
        ignoreStreakCount = 0
    }

    /** Called when the user voluntarily starts a game from the menu (non-invitation path). */
    fun recordManualGameStarted(nowMs: Long) {
        // Treat a manual start like a game completion for suppression purposes.
        lastGameCompletedAtMs = nowMs
        lastInvitationSentAtMs = nowMs
        ignoreStreakCount = maxOf(0, ignoreStreakCount - 1) // Partial streak reduction
    }

    companion object {
        /** Baseline interval between autonomous invitations (5 minutes). */
        private const val MIN_INVITATION_INTERVAL_MS = 5 * 60_000L

        /** After this many consecutive ignores, suppression interval doubles. */
        private const val MAX_IGNORE_STREAK = 3

        /** Extended suppression after hitting the ignore streak threshold (10 minutes). */
        private const val IGNORE_SUPPRESSION_MS = 10 * 60_000L

        /** Suppression after any completed game — pet is satisfied (8 minutes). */
        private const val POST_PLAY_SUPPRESSION_MS = 8 * 60_000L
    }
}
