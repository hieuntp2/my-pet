package com.aipet.brain.ui.avatar.model

/**
 * Payload for a pet talking/thought bubble.
 * Text must be short (pet speech, not app notifications).
 * Duration controls auto-dismiss timing.
 */
data class PetBubblePayload(
    val text: String,
    val durationMs: Long = DEFAULT_DURATION_MS
) {
    init {
        require(text.isNotBlank()) { "Bubble text must not be blank." }
        require(durationMs > 0) { "Bubble duration must be positive." }
    }

    companion object {
        const val DEFAULT_DURATION_MS: Long = 3_500L
        const val SHORT_DURATION_MS: Long = 2_000L
        const val LONG_DURATION_MS: Long = 5_000L
    }
}
