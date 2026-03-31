package com.aipet.brain.brain.fusion

/**
 * Social meaning derived from presence, gaze approximation, and interaction quality.
 */
data class SocialContext(
    /** True if the user appears to be looking toward the pet. */
    val userWatchingPet: Boolean = false,
    /** 0–1: likelihood of direct eye contact based on face angle + position. */
    val eyeContactLikelihood: Float = 0f,
    /** 0–1: how available the user seems for social interaction. */
    val interactionAvailability: Float = 0f,
    /** 0–1: warmth signal from recent positive interactions. */
    val socialWarmthSignal: Float = 0f
) {
    companion object {
        val DEFAULT = SocialContext()
    }
}
