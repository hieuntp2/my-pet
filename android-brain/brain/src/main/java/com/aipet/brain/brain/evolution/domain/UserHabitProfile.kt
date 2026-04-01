package com.aipet.brain.brain.evolution.domain

/**
 * Learned user behavioral rhythm.
 * Feeds expectation logic and reunion warmth calibration.
 */
data class UserHabitProfile(
    val id: String = "singleton",
    /**
     * Comma-separated daypart names observed most frequently.
     * Values: MORNING, DAY, EVENING, NIGHT
     */
    val preferredTimeSlotsJson: String,
    val avgSessionLengthMs: Long,
    val avgSessionsPerDay: Float,
    /**
     * Dominant interaction type observed across sessions.
     * Values: TAP, FEED, PLAY, COMFORT, IDLE, MIXED
     */
    val primaryInteractionStyle: String,
    /** 0..1: how predictable the user's schedule is. */
    val recentConsistencyScore: Float,
    /** The single daypart with highest observed session frequency. */
    val strongestDaypart: String,
    val lastUpdatedAtMs: Long
) {
    companion object {
        val DEFAULT = UserHabitProfile(
            preferredTimeSlotsJson = "[]",
            avgSessionLengthMs = 0L,
            avgSessionsPerDay = 0f,
            primaryInteractionStyle = "MIXED",
            recentConsistencyScore = 0f,
            strongestDaypart = "NONE",
            lastUpdatedAtMs = 0L
        )
    }
}
