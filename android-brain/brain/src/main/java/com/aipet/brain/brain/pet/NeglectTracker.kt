package com.aipet.brain.brain.pet

/**
 * Detects and applies neglect impact to pet state.
 *
 * Neglect in this system is NOT about punishing the user.
 * It is about the pet's emotional memory of inconsistency:
 * - the relationship cools slightly
 * - trust recovers more slowly
 * - greetings become hesitant until repaired
 *
 * A single long absence is NOT enough — it must be a pattern.
 */
class NeglectTracker {

    /**
     * Evaluates whether the current return qualifies as a neglect episode and applies
     * appropriate state changes. Called at app-open after decay is applied.
     *
     * @param state state after decay was applied
     * @param absenceBucket the classified absence window
     * @param now current timestamp
     * @return updated state with neglect changes applied (or unchanged if no neglect)
     */
    fun applyIfNeeded(
        state: PetState,
        absenceBucket: AbsenceBucket
    ): PetState {
        // Only long or neglect returns trigger neglect impact
        if (absenceBucket != AbsenceBucket.LONG_RETURN && absenceBucket != AbsenceBucket.NEGLECT_RETURN) {
            return state
        }

        // If the user has been consistently caring, don't trigger neglect for one long gap
        if (state.careStreak >= PetEmotionalConfig.NEGLECT_CARE_STREAK_RESET_THRESHOLD &&
            state.neglectStreak == 0
        ) {
            return state
        }

        val baseNeglect = if (absenceBucket == AbsenceBucket.NEGLECT_RETURN) 2 else 1
        val newNeglectStreak = (state.neglectStreak + baseNeglect)
            .coerceAtMost(PetEmotionalConfig.NEGLECT_STREAK_MAX)

        // Neglect reduces trust more than bond — trust is fragile
        val trustDrop = PetEmotionalConfig.NEGLECT_TRUST_DROP * baseNeglect
        val valenceDrop = PetEmotionalConfig.NEGLECT_VALENCE_DROP

        // Care streak resets when neglect accumulates
        val newCareStreak = if (newNeglectStreak >= PetEmotionalConfig.NEGLECT_CARE_STREAK_RESET_THRESHOLD) {
            (state.careStreak - 1).coerceAtLeast(0)
        } else {
            state.careStreak
        }

        return state.copy(
            trustScore = (state.trustScore - trustDrop).coerceAtLeast(PetState.VALUE_MIN),
            moodValence = (state.moodValence - valenceDrop).coerceAtLeast(PetState.VALENCE_MIN),
            neglectStreak = newNeglectStreak,
            careStreak = newCareStreak
        ).withClampedValues(lastUpdatedAt = state.lastUpdatedAt)
    }

    /**
     * Determines if this neglect application should emit an event.
     */
    fun shouldEmitEvent(originalNeglectStreak: Int, newNeglectStreak: Int): Boolean {
        return newNeglectStreak > originalNeglectStreak
    }
}
