package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait

/**
 * Scores behavior family candidates using the full v2 state model.
 *
 * Design rule: every candidate's score should be explainable.
 * The scoring engine produces an explanation breakdown alongside the winner.
 *
 * Personality traits act as multipliers on candidate base scores,
 * making the same state produce slightly different peak behavior for different pets.
 */
class PetBehaviorScoringEngine {

    data class ScoredCandidate(
        val family: PetBehaviorFamily,
        val score: Float,
        val reasons: List<String>
    )

    data class ScoringResult(
        val winner: PetBehaviorFamily,
        val rankedCandidates: List<ScoredCandidate>,
        val suppressionNotes: List<String>
    )

    fun score(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        absenceBucket: AbsenceBucket,
        sessionContext: SessionInteractionState
    ): ScoringResult {
        val suppressionNotes = mutableListOf<String>()
        val rawCandidates = buildCandidates(state, conditions, traits, absenceBucket, sessionContext, suppressionNotes)
        val ranked = rawCandidates.sortedByDescending { it.score }
        val winner = ranked.first().family
        return ScoringResult(
            winner = winner,
            rankedCandidates = ranked,
            suppressionNotes = suppressionNotes
        )
    }

    private fun buildCandidates(
        state: PetState,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        absenceBucket: AbsenceBucket,
        session: SessionInteractionState,
        suppressionNotes: MutableList<String>
    ): List<ScoredCandidate> {

        val isSleepy = conditions.contains(PetCondition.SLEEPY)
        val isHungry = conditions.contains(PetCondition.HUNGRY)
        val isDistant = conditions.contains(PetCondition.DISTANT)
        val isOverstimulated = conditions.contains(PetCondition.OVERSTIMULATED)
        val isNeedy = conditions.contains(PetCondition.NEEDY)
        val isBored = conditions.contains(PetCondition.BORED)
        val isPlayful = conditions.contains(PetCondition.PLAYFUL)

        val playfulBias = traits?.playful ?: 0.5f
        val sociabilityBias = traits?.sociability ?: 0.5f
        val attachmentBias = traits?.attachment ?: 0.5f
        val patienceBias = traits?.patience ?: 0.5f

        val candidates = mutableListOf<ScoredCandidate>()

        // ── GREET_WARM ────────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (state.bond >= 40) { score += 0.4f; reasons += "bond=${state.bond}" }
            if (state.trustScore >= 30) { score += 0.3f; reasons += "trust=${state.trustScore}" }
            if (absenceBucket == AbsenceBucket.MEDIUM_RETURN || absenceBucket == AbsenceBucket.LONG_RETURN) {
                score += 0.2f; reasons += "absence=$absenceBucket"
            }
            score += sociabilityBias * 0.15f
            score += attachmentBias * 0.1f
            if (isSleepy) { score -= 0.4f; suppressionNotes += "GREET_WARM suppressed by sleepiness" }
            if (isDistant) { score -= 0.6f; suppressionNotes += "GREET_WARM suppressed by distant" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.GREET_WARM, score, reasons)
        }

        // ── GREET_GENTLE ──────────────────────────────────────────────────────
        run {
            var score = 0.3f  // baseline — most greetings land here
            val reasons = mutableListOf("baseline_gentle")
            if (state.bond in 15..50) { score += 0.2f; reasons += "moderate_bond" }
            score += sociabilityBias * 0.1f
            if (isSleepy) score -= 0.15f
            if (isDistant) score -= 0.3f
            candidates += ScoredCandidate(PetBehaviorFamily.GREET_GENTLE, score, reasons)
        }

        // ── GREET_SLEEPY ──────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isSleepy) { score += 0.8f; reasons += "sleepy_condition" }
            if (state.sleepiness >= 60) { score += 0.3f; reasons += "sleepiness=${state.sleepiness}" }
            if (state.energy <= 30) { score += 0.2f; reasons += "low_energy=${state.energy}" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.GREET_SLEEPY, score, reasons)
        }

        // ── GREET_DISTANT ─────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isDistant) { score += 0.7f; reasons += "distant_condition" }
            if (state.neglectStreak >= 3) { score += 0.3f; reasons += "neglect=${ state.neglectStreak}" }
            if (state.trustScore <= 20) { score += 0.2f; reasons += "low_trust=${state.trustScore}" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.GREET_DISTANT, score, reasons)
        }

        // ── SEEK_COMFORT ──────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isNeedy) { score += 0.5f; reasons += "needy_condition" }
            if (state.comfort <= 40) { score += 0.3f; reasons += "low_comfort=${state.comfort}" }
            if (state.trustScore in 20..50) { score += 0.2f; reasons += "moderate_trust=${state.trustScore}" }
            score += attachmentBias * 0.2f
            if (isSleepy) score -= 0.2f
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.SEEK_COMFORT, score, reasons)
        }

        // ── INVITE_PLAY ───────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isBored) { score += 0.4f; reasons += "bored_condition" }
            if (isPlayful) { score += 0.5f; reasons += "playful_condition" }
            score += playfulBias * 0.3f
            if (state.stimulation <= 40) { score += 0.2f; reasons += "low_stim=${state.stimulation}" }
            // Suppress if not enough energy, sleepy, or distant
            if (isSleepy || state.energy < PetEmotionalConfig.INVITATION_MIN_ENERGY) {
                score -= 0.5f; suppressionNotes += "INVITE_PLAY low energy/sleepy"
            }
            if (isDistant) { score -= 0.4f; suppressionNotes += "INVITE_PLAY suppressed by distant" }
            if (session.invitationCount >= PetEmotionalConfig.INVITATION_PER_SESSION_MAX) {
                score -= 0.8f; suppressionNotes += "INVITE_PLAY session invite cap"
            }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.INVITE_PLAY, score, reasons)
        }

        // ── ASK_FOR_FOOD ──────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isHungry) { score += 0.8f; reasons += "hungry_condition" }
            if (state.hunger >= 60) { score += 0.2f; reasons += "hunger=${state.hunger}" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.ASK_FOR_FOOD, score, reasons)
        }

        // ── SETTLE_CALMLY ─────────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            val isCalm = conditions.contains(PetCondition.CALM)
            if (isCalm) { score += 0.5f; reasons += "calm_condition" }
            if (state.comfort >= 60 && state.trustScore >= 30) {
                score += 0.3f; reasons += "comfortable_and_trusted"
            }
            if (isSleepy) { score += 0.15f; reasons += "sleepy_settling" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.SETTLE_CALMLY, score, reasons)
        }

        // ── CURIOUS_EXPLORE ───────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isBored) { score += 0.3f; reasons += "bored_condition" }
            val curiosityBias = traits?.curious ?: 0.5f
            score += curiosityBias * 0.35f
            if (state.stimulation in 20..55) { score += 0.2f; reasons += "moderate_stim" }
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.CURIOUS_EXPLORE, score, reasons)
        }

        // ── WITHDRAW_SLIGHTLY ─────────────────────────────────────────────────
        run {
            var score = 0f
            val reasons = mutableListOf<String>()
            if (isOverstimulated) { score += 0.7f; reasons += "overstimulated_condition" }
            if (state.stimulation >= 75) { score += 0.2f; reasons += "high_stim=${state.stimulation}" }
            val inversePatienceBonus = (1f - patienceBias) * 0.2f
            score += inversePatienceBonus
            if (score > 0f) candidates += ScoredCandidate(PetBehaviorFamily.WITHDRAW_SLIGHTLY, score, reasons)
        }

        // ── RELIEF_AFTER_SOOTHE ───────────────────────────────────────────────
        run {
            if (session.wasComfortedThisSession && state.comfort >= 60) {
                candidates += ScoredCandidate(
                    PetBehaviorFamily.RELIEF_AFTER_SOOTHE,
                    0.6f,
                    listOf("comforted_this_session")
                )
            }
        }

        // Ensure at least one candidate exists
        if (candidates.isEmpty()) {
            candidates += ScoredCandidate(PetBehaviorFamily.GREET_GENTLE, 0.1f, listOf("fallback"))
        }

        return candidates
    }
}

/**
 * Lightweight session interaction state for the behavior scorer.
 * Lives in app-layer memory — not persisted.
 */
data class SessionInteractionState(
    val invitationCount: Int = 0,
    val ignoredInvitationCount: Int = 0,
    val wasComfortedThisSession: Boolean = false,
    val lastInvitationAtMs: Long = 0L,
    val sessionInteractionCount: Int = 0
)
