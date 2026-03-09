package com.aipet.brain.perception.audio.model

/**
 * Runtime energy metrics derived from an audio frame.
 * Values are intentionally generic so both simple and advanced estimators can share this contract.
 */
data class AudioEnergyMetrics(
    val frameTimestampMs: Long,
    val sampleRate: Int,
    val channelCount: Int,
    val sampleCount: Int,
    val rms: Double,
    val peak: Double,
    val smoothed: Double
) {
    init {
        require(frameTimestampMs >= 0L) { "frameTimestampMs must be >= 0" }
        require(sampleRate > 0) { "sampleRate must be > 0" }
        require(channelCount > 0) { "channelCount must be > 0" }
        require(sampleCount >= 0) { "sampleCount must be >= 0" }
        require(rms >= 0.0) { "rms must be >= 0" }
        require(peak >= 0.0) { "peak must be >= 0" }
        require(smoothed >= 0.0) { "smoothed must be >= 0" }
    }
}
