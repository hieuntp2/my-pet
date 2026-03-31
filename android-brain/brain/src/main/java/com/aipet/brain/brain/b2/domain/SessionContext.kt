package com.aipet.brain.brain.b2.domain

/**
 * Holds contextual information about the current session
 * (how long it has been running, novelty, etc.).
 */
data class SessionContext(
    val sessionStartMs: Long,
    /** Monotonically increasing age of the session in ms. */
    val sessionAgeMs: Long,
    /** Hour of day 0–23. */
    val hourOfDay: Int,
    /** Whether this is the pet's first greeting of the day. */
    val isFirstGreetToday: Boolean,
    /** How many interactions have occurred in this session. */
    val sessionInteractionCount: Int,
    /** How many ms since the user last interacted directly. */
    val msSinceLastDirectInteraction: Long,
    /** True if the user was absent for more than [LONG_ABSENCE_THRESHOLD_MS]. */
    val isReturningAfterLongAbsence: Boolean,
    val nowMs: Long
) {
    val isNightTime: Boolean get() = hourOfDay in 22..23 || hourOfDay in 0..5
    val isMorning: Boolean get() = hourOfDay in 6..10
    val isAfternoon: Boolean get() = hourOfDay in 11..17

    /** Session novelty score: high early in session, decays over time. */
    val noveltyScore: Float
        get() {
            val decayMinutes = sessionAgeMs / 60_000f
            return (1f - decayMinutes / NOVELTY_DECAY_MINUTES).coerceIn(0f, 1f)
        }

    companion object {
        private const val NOVELTY_DECAY_MINUTES = 15f
        const val LONG_ABSENCE_THRESHOLD_MS = 3_600_000L // 1 hour
    }
}
