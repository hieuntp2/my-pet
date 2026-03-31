package com.aipet.brain.brain.fusion

/**
 * Stable user presence state inferred from camera + tracking continuity.
 * Uses temporal smoothing to avoid flickering.
 */
data class PresenceState(
    val userPresent: Boolean = false,
    val familiarUserPresent: Boolean = false,
    val recognizedPersonId: String? = null,
    val faceCount: Int = 0,
    /** How long the user has been stably present in ms. */
    val stablePresenceMs: Long = 0L,
    /** How long the user has been continuously absent in ms. */
    val absenceMs: Long = 0L,
    val entryEventRecently: Boolean = false,
    val exitEventRecently: Boolean = false,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = PresenceState()
    }
}
