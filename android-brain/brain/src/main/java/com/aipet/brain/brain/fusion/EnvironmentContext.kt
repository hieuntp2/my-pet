package com.aipet.brain.brain.fusion

/**
 * High-level environmental context summarizing the overall interaction pressure
 * and ambient state.
 */
data class EnvironmentContext(
    /** True if no significant visual events are occurring. */
    val visualQuiet: Boolean = true,
    /** True if no significant audio events are occurring. */
    val audioQuiet: Boolean = true,
    /** 0–1: combined pressure to interact (faces + sounds + touch). */
    val interactionPressure: Float = 0f
) {
    companion object {
        val DEFAULT = EnvironmentContext()
    }
}
