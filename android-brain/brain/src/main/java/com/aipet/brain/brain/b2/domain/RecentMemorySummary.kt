package com.aipet.brain.brain.b2.domain

/**
 * Snapshot of recent perception history used by the brain for contextual decision-making.
 * This is the semantic short-term memory of what was recently perceived.
 */
data class RecentMemorySummary(
    /** True if the user entered the scene recently (within threshold). */
    val userEnteredRecentlyMs: Long = 0L,
    /** True if the user was last seen absent for more than a short time. */
    val userLastSeenMs: Long = 0L,
    /** Most recent person ID seen, if known. */
    val lastRecognizedPersonId: String? = null,
    /** True if there was a loud sound spike recently. */
    val loudSoundRecentlyMs: Long = 0L,
    /** ms since last direct touch from user. */
    val lastTouchMs: Long = 0L,
    /** ms since last voice command was parsed. */
    val lastVoiceCommandMs: Long = 0L,
    /** ms since last game ended. */
    val lastGameEndMs: Long = 0L,
    /** ms since last time pet belly was rubbed / long press. */
    val lastLongPressMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    fun userEnteredWithinMs(windowMs: Long, nowMs: Long): Boolean =
        userEnteredRecentlyMs > 0L && (nowMs - userEnteredRecentlyMs) < windowMs

    fun hadTouchWithinMs(windowMs: Long, nowMs: Long): Boolean =
        lastTouchMs > 0L && (nowMs - lastTouchMs) < windowMs

    fun hadVoiceWithinMs(windowMs: Long, nowMs: Long): Boolean =
        lastVoiceCommandMs > 0L && (nowMs - lastVoiceCommandMs) < windowMs

    fun hadLoudSoundWithinMs(windowMs: Long, nowMs: Long): Boolean =
        loudSoundRecentlyMs > 0L && (nowMs - loudSoundRecentlyMs) < windowMs

    companion object {
        val DEFAULT = RecentMemorySummary()
    }
}
