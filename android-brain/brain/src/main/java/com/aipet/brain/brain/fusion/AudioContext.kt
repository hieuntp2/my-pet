package com.aipet.brain.brain.fusion

/**
 * Audio environment context fused from raw energy and sound events.
 */
data class AudioContext(
    /** 0–1: normalized ambient sound level. */
    val ambientLevel: Float = 0f,
    /** True if a loud event is currently active or in recovery window. */
    val loudEventActive: Boolean = false,
    /** 0–1: how likely the current sound is from an external source. */
    val externalSoundConfidence: Float = 0f,
    /** True if the pet is currently playing back its own audio. */
    val selfPlaybackActive: Boolean = false,
    /** True if the sound is worth orienting toward (high enough confidence + loud). */
    val shouldOrientToSound: Boolean = false,
    /** When the most recent loud sound event started (0 if none recent). */
    val loudSoundStartMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = AudioContext()
    }
}
