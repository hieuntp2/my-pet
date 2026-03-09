package com.aipet.brain.perception.audio

import com.aipet.brain.perception.audio.model.AudioEnergyMetrics
import com.aipet.brain.perception.audio.model.AudioFrame
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Computes lightweight runtime energy metrics from PCM16 microphone frames.
 *
 * Pipeline position: AudioFrameSource -> EnergyEstimator -> VAD/event stages.
 * The estimator operates on normalized amplitudes ([0..1]) so values remain stable
 * across sample rates and frame sizes.
 */
class EnergyEstimator(
    private val smoothingAlpha: Double = DEFAULT_SMOOTHING_ALPHA
) {
    init {
        require(smoothingAlpha in 0.0..1.0) { "smoothingAlpha must be in [0,1]" }
    }

    @Synchronized
    fun estimate(frame: AudioFrame): AudioEnergyMetrics {
        val sampleCount = frame.sampleCount.coerceAtMost(frame.pcmData.size)
        if (sampleCount <= 0) {
            val smoothed = if (hasSmoothedValue) smoothedRms else 0.0
            return AudioEnergyMetrics(
                frameTimestampMs = frame.timestampMs,
                sampleRate = frame.sampleRate,
                channelCount = frame.channelCount,
                sampleCount = 0,
                rms = 0.0,
                peak = 0.0,
                smoothed = smoothed
            )
        }

        var sumSquares = 0.0
        var peakAmplitude = 0.0
        for (index in 0 until sampleCount) {
            val sample = frame.pcmData[index].toInt()
            val amplitude = if (sample == Short.MIN_VALUE.toInt()) {
                MAX_PCM_AMPLITUDE
            } else {
                abs(sample).toDouble()
            }
            if (amplitude > peakAmplitude) {
                peakAmplitude = amplitude
            }

            val normalized = sample / MAX_PCM_AMPLITUDE
            sumSquares += normalized * normalized
        }

        val rms = sqrt(sumSquares / sampleCount.toDouble()).coerceIn(0.0, 1.0)
        val peak = (peakAmplitude / MAX_PCM_AMPLITUDE).coerceIn(0.0, 1.0)
        smoothedRms = if (!hasSmoothedValue) {
            hasSmoothedValue = true
            rms
        } else {
            (smoothingAlpha * rms) + ((1.0 - smoothingAlpha) * smoothedRms)
        }

        return AudioEnergyMetrics(
            frameTimestampMs = frame.timestampMs,
            sampleRate = frame.sampleRate,
            channelCount = frame.channelCount,
            sampleCount = sampleCount,
            rms = rms,
            peak = peak,
            smoothed = smoothedRms
        )
    }

    @Synchronized
    fun reset() {
        smoothedRms = 0.0
        hasSmoothedValue = false
    }

    private var smoothedRms: Double = 0.0
    private var hasSmoothedValue: Boolean = false

    companion object {
        private const val MAX_PCM_AMPLITUDE = 32768.0
        private const val DEFAULT_SMOOTHING_ALPHA = 0.25
    }
}
