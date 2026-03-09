package com.aipet.brain.perception.audio.model

/**
 * One PCM frame emitted by an [com.aipet.brain.perception.audio.AudioFrameSource].
 * Detectors consume this unit for energy analysis, VAD-light, and future plugins.
 */
data class AudioFrame(
    val timestampMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    val sampleCount: Int,
    val pcmData: ShortArray
) {
    init {
        require(sampleRate > 0) { "sampleRate must be > 0" }
        require(channelCount > 0) { "channelCount must be > 0" }
        require(sampleCount >= 0) { "sampleCount must be >= 0" }
        require(sampleCount <= pcmData.size) { "sampleCount must be <= pcmData size" }
    }
}
