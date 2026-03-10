package com.aipet.brain.perception.audio.keyword

import com.aipet.brain.perception.audio.model.AudioFrame
import com.aipet.brain.perception.audio.model.KeywordDetectionResult
import com.aipet.brain.perception.audio.model.KeywordDetectionType
import com.aipet.brain.perception.audio.model.KeywordSpotterState
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * A lightweight on-device keyword spotter that detects a wake-word-like
 * two-syllable acoustic pattern from the shared AudioRecord frame stream.
 *
 * This is intentionally simple for the first real integration. It operates on:
 * - frame RMS (signal energy)
 * - syllable-like peak counting with hysteresis
 * - utterance duration windowing
 */
class AcousticPatternKeywordSpotter(
    private val config: AcousticPatternKeywordSpotterConfig = AcousticPatternKeywordSpotterConfig()
) : KeywordSpotter {
    override val spotterId: String = config.engineName

    override fun start() {
        synchronized(lock) {
            if (state == KeywordSpotterState.RUNNING || state == KeywordSpotterState.STARTING) {
                return
            }
            state = KeywordSpotterState.STARTING
            resetInternalState()
            state = KeywordSpotterState.RUNNING
        }
    }

    override fun stop() {
        synchronized(lock) {
            if (state == KeywordSpotterState.IDLE || state == KeywordSpotterState.STOPPING) {
                return
            }
            state = KeywordSpotterState.STOPPING
            resetInternalState()
            state = KeywordSpotterState.IDLE
        }
    }

    override fun reset() {
        synchronized(lock) {
            resetInternalState()
            if (state == KeywordSpotterState.FAILED) {
                state = KeywordSpotterState.IDLE
            }
        }
    }

    override fun state(): KeywordSpotterState {
        synchronized(lock) {
            return state
        }
    }

    override fun processFrame(frame: AudioFrame): KeywordDetectionResult? {
        synchronized(lock) {
            if (state != KeywordSpotterState.RUNNING) {
                return null
            }
            if (frame.sampleCount <= 0) {
                return null
            }

            val frameRms = computeRms(frame)
            smoothedRms = if (smoothedRms == 0.0) {
                frameRms
            } else {
                (config.smoothingAlpha * frameRms) + ((1.0 - config.smoothingAlpha) * smoothedRms)
            }

            val frameTimestampMs = frame.timestampMs
            val voiceDetected = if (utteranceActive) {
                smoothedRms >= config.voiceHoldRmsThreshold
            } else {
                smoothedRms >= config.voiceStartRmsThreshold
            }

            if (voiceDetected) {
                onVoiceFrameDetected(frameTimestampMs)
                return null
            }

            if (utteranceActive && (frameTimestampMs - lastVoiceFrameTimestampMs) >= config.silenceToFinalizeMs) {
                return finalizeUtterance(frameTimestampMs)
            }
            return null
        }
    }

    override fun setDetectionListener(listener: KeywordDetectionListener?) {
        synchronized(lock) {
            detectionListener = listener
        }
    }

    private fun onVoiceFrameDetected(frameTimestampMs: Long) {
        if (!utteranceActive) {
            utteranceActive = true
            utteranceStartTimestampMs = frameTimestampMs
            lastVoiceFrameTimestampMs = frameTimestampMs
            peakCount = 0
            maxSmoothedRms = smoothedRms
            peakGateOpen = true
            lastPeakTimestampMs = Long.MIN_VALUE
        } else {
            lastVoiceFrameTimestampMs = frameTimestampMs
            maxSmoothedRms = max(maxSmoothedRms, smoothedRms)
        }

        if (peakGateOpen && smoothedRms >= config.peakEnterRmsThreshold) {
            val elapsedSincePeak = frameTimestampMs - lastPeakTimestampMs
            if (elapsedSincePeak >= config.minPeakDistanceMs) {
                peakCount += 1
                lastPeakTimestampMs = frameTimestampMs
            }
            peakGateOpen = false
        } else if (!peakGateOpen && smoothedRms <= config.peakExitRmsThreshold) {
            peakGateOpen = true
        }
    }

    private fun finalizeUtterance(frameTimestampMs: Long): KeywordDetectionResult? {
        val utteranceDurationMs = (lastVoiceFrameTimestampMs - utteranceStartTimestampMs)
            .coerceAtLeast(0L)

        val passesDuration = utteranceDurationMs in config.minUtteranceDurationMs..config.maxUtteranceDurationMs
        val passesPeakCount = peakCount in config.minSyllablePeaks..config.maxSyllablePeaks
        val passesEnergy = maxSmoothedRms >= config.minUtterancePeakRms
        val inCooldown = frameTimestampMs < cooldownUntilTimestampMs

        val result = if (!inCooldown && passesDuration && passesPeakCount && passesEnergy) {
            val confidence = computeConfidence(
                utteranceDurationMs = utteranceDurationMs,
                peakCount = peakCount,
                maxSmoothedRms = maxSmoothedRms
            )
            val detection = KeywordDetectionResult(
                detectionType = KeywordDetectionType.WAKE_WORD,
                keywordId = config.keywordId,
                keywordText = config.keywordText,
                confidence = confidence,
                timestampMs = System.currentTimeMillis(),
                engineName = spotterId
            )
            cooldownUntilTimestampMs = frameTimestampMs + config.detectionCooldownMs
            detectionListener?.onKeywordDetected(detection)
            detection
        } else {
            null
        }

        resetUtteranceWindow()
        return result
    }

    private fun computeConfidence(
        utteranceDurationMs: Long,
        peakCount: Int,
        maxSmoothedRms: Double
    ): Float {
        val durationScore = 1.0 - (
            abs(utteranceDurationMs - config.targetUtteranceDurationMs).toDouble() /
                config.targetUtteranceDurationMs.toDouble()
            )
        val peakScore = 1.0 - (
            abs(peakCount - config.targetSyllablePeaks).toDouble() /
                config.targetSyllablePeaks.toDouble()
            )
        val energyScore = ((maxSmoothedRms - config.minUtterancePeakRms) /
            (config.maxExpectedPeakRms - config.minUtterancePeakRms))
            .coerceIn(0.0, 1.0)

        val weighted = (durationScore.coerceIn(0.0, 1.0) * 0.40) +
            (peakScore.coerceIn(0.0, 1.0) * 0.35) +
            (energyScore * 0.25)
        return weighted.toFloat().coerceIn(0f, 1f)
    }

    private fun computeRms(frame: AudioFrame): Double {
        var sumSquares = 0.0
        var sampleIndex = 0
        while (sampleIndex < frame.sampleCount) {
            val normalized = frame.pcmData[sampleIndex] / PCM_NORMALIZER
            sumSquares += normalized * normalized
            sampleIndex += 1
        }
        return sqrt(sumSquares / frame.sampleCount.toDouble())
    }

    private fun resetInternalState() {
        resetUtteranceWindow()
        smoothedRms = 0.0
        cooldownUntilTimestampMs = 0L
    }

    private fun resetUtteranceWindow() {
        utteranceActive = false
        utteranceStartTimestampMs = 0L
        lastVoiceFrameTimestampMs = 0L
        peakCount = 0
        maxSmoothedRms = 0.0
        peakGateOpen = true
        lastPeakTimestampMs = Long.MIN_VALUE
    }

    private val lock = Any()
    private var state: KeywordSpotterState = KeywordSpotterState.IDLE
    private var detectionListener: KeywordDetectionListener? = null

    private var smoothedRms: Double = 0.0
    private var utteranceActive: Boolean = false
    private var utteranceStartTimestampMs: Long = 0L
    private var lastVoiceFrameTimestampMs: Long = 0L
    private var peakCount: Int = 0
    private var maxSmoothedRms: Double = 0.0
    private var peakGateOpen: Boolean = true
    private var lastPeakTimestampMs: Long = Long.MIN_VALUE
    private var cooldownUntilTimestampMs: Long = 0L

    companion object {
        private const val PCM_NORMALIZER = 32768.0
    }
}

data class AcousticPatternKeywordSpotterConfig(
    val engineName: String = "custom_acoustic_pattern",
    val keywordId: String = "hey_pet",
    val keywordText: String = "hey pet",
    val smoothingAlpha: Double = 0.22,
    val voiceStartRmsThreshold: Double = 0.040,
    val voiceHoldRmsThreshold: Double = 0.028,
    val peakEnterRmsThreshold: Double = 0.060,
    val peakExitRmsThreshold: Double = 0.035,
    val minPeakDistanceMs: Long = 140L,
    val silenceToFinalizeMs: Long = 180L,
    val minUtteranceDurationMs: Long = 300L,
    val maxUtteranceDurationMs: Long = 1_300L,
    val targetUtteranceDurationMs: Long = 650L,
    val minSyllablePeaks: Int = 2,
    val maxSyllablePeaks: Int = 4,
    val targetSyllablePeaks: Int = 2,
    val minUtterancePeakRms: Double = 0.070,
    val maxExpectedPeakRms: Double = 0.250,
    val detectionCooldownMs: Long = 1_500L
)
