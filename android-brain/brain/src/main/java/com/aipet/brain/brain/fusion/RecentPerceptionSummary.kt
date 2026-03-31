package com.aipet.brain.brain.fusion

/**
 * Short-term semantic memory of recent perception events.
 * Allows the behavior engine to reason with temporal continuity.
 */
data class RecentPerceptionSummary(
    val userEnteredAtMs: Long = 0L,
    val userExitedAtMs: Long = 0L,
    val soundSpikeAtMs: Long = 0L,
    val voiceCommandAtMs: Long = 0L,
    val longPressEndedAtMs: Long = 0L,
    val tapAtMs: Long = 0L,
    val userAbsentForMs: Long = 0L
) {
    fun userEnteredWithinMs(windowMs: Long, nowMs: Long): Boolean =
        userEnteredAtMs > 0L && (nowMs - userEnteredAtMs) < windowMs

    fun hadSoundWithinMs(windowMs: Long, nowMs: Long): Boolean =
        soundSpikeAtMs > 0L && (nowMs - soundSpikeAtMs) < windowMs

    fun hadVoiceWithinMs(windowMs: Long, nowMs: Long): Boolean =
        voiceCommandAtMs > 0L && (nowMs - voiceCommandAtMs) < windowMs

    fun hadTouchWithinMs(windowMs: Long, nowMs: Long): Boolean =
        tapAtMs > 0L && (nowMs - tapAtMs) < windowMs

    companion object {
        val DEFAULT = RecentPerceptionSummary()
    }
}
