package com.aipet.brain.brain.fusion

/**
 * Current directionality of pet attention, synthesized from multiple signals.
 */
enum class FocusDirection {
    CENTER,
    UP,
    DOWN,
    LEFT,
    RIGHT,
    AMBIENT,
    UNKNOWN
}

/**
 * What the pet likely should be orienting towards right now.
 */
data class AttentionContext(
    val focusDirection: FocusDirection = FocusDirection.AMBIENT,
    /** 0–1: novelty of recent stimulus; drives orienting behavior. */
    val noveltySignal: Float = 0f,
    /** 0–1: urgency to reorient (high = something important happened). */
    val orientingUrgency: Float = 0f
) {
    companion object {
        val DEFAULT = AttentionContext()
    }
}
