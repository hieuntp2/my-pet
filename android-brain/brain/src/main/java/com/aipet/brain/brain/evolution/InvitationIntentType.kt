package com.aipet.brain.brain.evolution

/**
 * Context-aware invitation intent types.
 * These replace the generic "invite to play" with emotionally specific bids.
 */
enum class InvitationIntentType {
    /** High energy, good care — classic playful bid. */
    PLAYFUL,
    /** Sensitivity/attachment high, slight instability — seeking closeness. */
    COMFORT_SEEKING,
    /** Dependency is high, pet was waiting. Associated with expected return window. */
    EXPECTANT,
    /** Night phase or low energy — soft, gentle, quiet invitation. */
    QUIET_PRESENCE,
    /** Recovery arc — tentative reach-out after trust damage. */
    TENTATIVE_REACH_OUT
}
