package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.UserHabitProfile

/**
 * Determines the pet's expectation state relative to the user's learned routine.
 * Feeds into reunion warmth and invitation intensity.
 */
class ExpectedReturnWindowResolver {

    enum class ExpectationState {
        /** The user's routine is not well-established yet. */
        UNKNOWN,
        /** The user is arriving at their expected time — pet feels ready. */
        ON_TIME,
        /** The user arrived slightly late but within tolerance. */
        SLIGHTLY_LATE,
        /** The user missed their expected window — pet was "waiting". */
        MISSED_WINDOW,
        /** Very early return — pleasant surprise. */
        EARLY
    }

    fun resolve(
        profile: UserHabitProfile,
        currentDayPhase: DayPhase,
        absenceMs: Long
    ): ExpectationState {
        if (profile.recentConsistencyScore < MIN_CONSISTENCY) return ExpectationState.UNKNOWN
        if (profile.strongestDaypart == "NONE") return ExpectationState.UNKNOWN

        val expectedPhase = profile.strongestDaypart
        val currentPhase = currentDayPhase.name

        return when {
            absenceMs < 20 * 60_000L -> ExpectationState.EARLY
            expectedPhase == currentPhase -> ExpectationState.ON_TIME
            isAdjacentPhase(expectedPhase, currentPhase) && absenceMs < 8 * 3600_000L -> ExpectationState.SLIGHTLY_LATE
            absenceMs > 8 * 3600_000L -> ExpectationState.MISSED_WINDOW
            else -> ExpectationState.UNKNOWN
        }
    }

    private fun isAdjacentPhase(expected: String, current: String): Boolean {
        val order = listOf("MORNING", "DAY", "EVENING", "NIGHT")
        val expectedIdx = order.indexOf(expected)
        val currentIdx = order.indexOf(current)
        return expectedIdx >= 0 && currentIdx >= 0 && Math.abs(expectedIdx - currentIdx) == 1
    }

    private companion object {
        const val MIN_CONSISTENCY = 0.3f
    }
}
