package com.aipet.brain.brain.evolution

/**
 * Shared anti-spam mapping for ignored invitation streaks.
 *
 * This keeps invitation suppression and scoring penalties aligned.
 */
internal object InvitationSuppressionRules {
    const val HARD_SUPPRESSION_IGNORED_COUNT = 4

    fun penaltyForIgnoredCount(ignoredCount: Int): Float = when {
        ignoredCount >= 3 -> 0.5f
        ignoredCount == 2 -> 0.3f
        ignoredCount == 1 -> 0.1f
        else -> 0f
    }
}
