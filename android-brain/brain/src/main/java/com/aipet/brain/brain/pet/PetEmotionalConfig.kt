package com.aipet.brain.brain.pet

/**
 * Central configuration for all emotional-life tuning constants.
 * Grouped by system area so balancing sessions can target one section at a time.
 * All thresholds are designed to feel organic at the product level, not just numeric.
 */
object PetEmotionalConfig {

    // ── Absence classification ────────────────────────────────────────────────
    const val ABSENCE_SHORT_MAX_MS: Long = 30 * 60_000L            // < 30 min
    const val ABSENCE_MEDIUM_MAX_MS: Long = 3 * 60 * 60_000L       // 30 min – 3 hr
    const val ABSENCE_LONG_MAX_MS: Long = 24 * 60 * 60_000L        // 3 hr – 24 hr
    // >= 24h with neglect streak >= NEGLECT_RETURN_STREAK → NEGLECT_RETURN
    const val NEGLECT_RETURN_STREAK_THRESHOLD: Int = 2

    // ── Care action state deltas ──────────────────────────────────────────────
    // Tap / pet
    const val TAP_SOCIAL_GAIN: Int = 5
    const val TAP_COMFORT_GAIN: Int = 3
    const val TAP_VALENCE_GAIN: Int = 4
    const val TAP_BOND_GAIN: Int = 1

    // Long press / hold
    const val HOLD_COMFORT_GAIN: Int = 8
    const val HOLD_TRUST_GAIN: Int = 4
    const val HOLD_SOCIAL_GAIN: Int = 6
    const val HOLD_AROUSAL_DROP: Int = 5

    // Soothe — direct emotional repair
    const val SOOTHE_COMFORT_GAIN: Int = 15
    const val SOOTHE_TRUST_GAIN: Int = 8
    const val SOOTHE_AROUSAL_DROP: Int = 10
    const val SOOTHE_VALENCE_GAIN: Int = 10
    const val SOOTHE_NEGLECT_STREAK_DROP: Int = 1

    // Play
    const val PLAY_STIMULATION_GAIN: Int = 20
    const val PLAY_SOCIAL_GAIN: Int = 8
    const val PLAY_VALENCE_GAIN: Int = 12
    const val PLAY_ENERGY_COST: Int = 8
    const val PLAY_BOND_GAIN: Int = 2

    // Feed
    const val FEED_HUNGER_DROP: Int = 40
    const val FEED_COMFORT_GAIN: Int = 10
    const val FEED_TRUST_GAIN: Int = 5
    const val FEED_VALENCE_GAIN: Int = 8

    // Linger (user stays in app without acting)
    const val LINGER_COMFORT_GAIN: Int = 3
    const val LINGER_TRUST_GAIN: Int = 2

    // ── Diminishing returns (anti-spam) ───────────────────────────────────────
    const val BURST_WINDOW_MS: Long = 30_000L          // 30-second burst window
    const val BURST_COUNT_DIMINISH_START: Int = 4      // full reward for first 3 taps
    const val BURST_MAX_STIMULATION_FROM_TAP: Int = 90 // cap stimulation from rapid taps
    const val BURST_COMFORT_COST_PER_EXCESS: Int = 2   // comfort drops on excess taps

    // ── Overstimulation ───────────────────────────────────────────────────────
    const val OVERSTIM_STIMULATION_THRESHOLD: Int = 85
    const val OVERSTIM_COMFORT_DROP: Int = 5
    const val OVERSTIM_TRUST_DROP: Int = 3

    // ── Neglect tracking ──────────────────────────────────────────────────────
    const val NEGLECT_ABSENCE_THRESHOLD_MS: Long = 8 * 60 * 60_000L  // 8 hours = notable absence
    const val NEGLECT_STREAK_MAX: Int = 10
    const val NEGLECT_TRUST_DROP: Int = 5
    const val NEGLECT_VALENCE_DROP: Int = 8
    const val NEGLECT_CARE_STREAK_RESET_THRESHOLD: Int = 2

    // ── Recovery ─────────────────────────────────────────────────────────────
    const val REPAIR_CARE_STREAK_GAIN: Int = 1
    const val REPAIR_NEGLECT_STREAK_DROP: Int = 1
    const val REPAIR_CARE_STREAK_TRUST_THRESHOLD: Int = 3   // after 3 care sessions, trust recovers faster

    // ── Invitation system ────────────────────────────────────────────────────
    const val INVITATION_MIN_SOCIAL_NEED: Int = 40      // invite only when socially wanting
    const val INVITATION_MIN_ATTACHMENT: Int = 20       // need some attachment to initiate
    const val INVITATION_MAX_SLEEPINESS: Int = 60       // don't invite when sleepy
    const val INVITATION_MIN_ENERGY: Int = 30          // need energy to invite
    const val INVITATION_MAX_STIMULATION: Int = 75      // don't invite if already stimulated
    const val INVITATION_COOLDOWN_MS: Long = 5 * 60_000L   // 5 min between invitations
    const val INVITATION_PER_SESSION_MAX: Int = 3        // max invitations per session
    const val INVITATION_IGNORED_SUPPRESS_COUNT: Int = 2 // suppress if 2+ ignored in session

    // ── Relationship stages ───────────────────────────────────────────────────
    const val STAGE_FAMILIAR_BOND_MIN: Int = 15
    const val STAGE_FAMILIAR_CARE_STREAK_MIN: Int = 2
    const val STAGE_ATTACHED_BOND_MIN: Int = 35
    const val STAGE_ATTACHED_TRUST_MIN: Int = 30
    const val STAGE_BONDED_BOND_MIN: Int = 65
    const val STAGE_BONDED_TRUST_MIN: Int = 55
    const val STAGE_BONDED_ATTACHMENT_MIN: Int = 50

    // ── Rare affection ────────────────────────────────────────────────────────
    const val RARE_COOLDOWN_MS: Long = 4 * 60 * 60_000L   // 4 hours min between rare moments
    const val RARE_MIN_BOND: Int = 40
    const val RARE_MIN_TRUST: Int = 35
    const val RARE_MIN_CARE_STREAK: Int = 3
    const val RARE_MAX_NEGLECT_STREAK: Int = 1
    const val RARE_MIN_VALENCE: Int = 20
}
