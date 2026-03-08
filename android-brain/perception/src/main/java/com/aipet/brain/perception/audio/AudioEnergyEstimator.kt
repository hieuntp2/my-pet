package com.aipet.brain.perception.audio

import kotlin.math.sqrt

class AudioEnergyEstimator {
    fun estimate(pcm16: ShortArray): AudioEnergyMetrics {
        if (pcm16.isEmpty()) {
            return AudioEnergyMetrics(
                rms = 0.0,
                peakAmplitude = 0,
                peakNormalized = 0.0
            )
        }

        var sumSquares = 0.0
        var peakAmplitude = 0
        for (sample in pcm16) {
            val amplitude = kotlin.math.abs(sample.toInt())
            if (amplitude > peakAmplitude) {
                peakAmplitude = amplitude
            }
            val normalizedSample = sample.toDouble() / Short.MAX_VALUE.toDouble()
            sumSquares += normalizedSample * normalizedSample
        }

        val rms = sqrt(sumSquares / pcm16.size.toDouble())
        val peakNormalized = peakAmplitude / Short.MAX_VALUE.toDouble()
        return AudioEnergyMetrics(
            rms = rms,
            peakAmplitude = peakAmplitude,
            peakNormalized = peakNormalized.coerceIn(0.0, 1.0)
        )
    }
}

data class AudioEnergyMetrics(
    val rms: Double,
    val peakAmplitude: Int,
    val peakNormalized: Double
)
