package com.aipet.brain.perception.audio.model

/**
 * VAD result for one processing step.
 */
data class VadResult(
    val state: VadState,
    val confidence: Float,
    val timestampMs: Long
) {
    init {
        require(confidence in 0f..1f) { "confidence must be in [0, 1]" }
    }
}
