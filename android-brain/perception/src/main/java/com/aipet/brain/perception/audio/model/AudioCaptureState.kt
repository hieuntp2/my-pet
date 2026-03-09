package com.aipet.brain.perception.audio.model

/**
 * Runtime capture state shared with higher layers (debug UI, event publishing, behavior integration).
 */
data class AudioCaptureState(
    val isRunning: Boolean,
    val sampleRate: Int,
    val channelCount: Int,
    val frameSize: Int
) {
    init {
        require(sampleRate >= 0) { "sampleRate must be >= 0" }
        require(channelCount >= 0) { "channelCount must be >= 0" }
        require(frameSize >= 0) { "frameSize must be >= 0" }
    }
}
