package com.aipet.brain.brain.fusion

/**
 * Social meaning derived from touch pattern analysis.
 */
data class TouchContext(
    val recentTap: Boolean = false,
    val recentLongPress: Boolean = false,
    /** 0–1: likelihood that recent touch was affectionate (not spammy). */
    val affectionLikelihood: Float = 0f,
    /** 0–1: likelihood that recent touch is a spam/annoyance pattern. */
    val spamLikelihood: Float = 0f,
    /** When was the most recent touch event (0 if none). */
    val lastTouchMs: Long = 0L,
    /** Number of taps in the recent rapid-tap window. */
    val recentTapCount: Int = 0,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = TouchContext()
    }
}
