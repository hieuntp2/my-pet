package com.aipet.brain.brain.logic.audio

import com.aipet.brain.brain.events.EventType

/**
 * Conservative policy for counting audio as meaningful activity for inactivity reset.
 *
 * This keeps sleepy/no-stimulus integration aligned with existing audio behavior heuristics:
 * - voice activity must be a valid STARTED signal with minimum confidence
 * - sound stimulus must exceed loud-energy thresholds
 */
class AudioMeaningfulStimulusPolicy(
    private val minVoiceConfidence: Float = DEFAULT_MIN_VOICE_CONFIDENCE,
    private val minSoundSmoothedEnergy: Double = DEFAULT_MIN_SOUND_SMOOTHED_ENERGY,
    private val minSoundPeakEnergy: Double = DEFAULT_MIN_SOUND_PEAK_ENERGY,
    private val minKeywordConfidence: Float = DEFAULT_MIN_KEYWORD_CONFIDENCE
) {
    init {
        require(minVoiceConfidence in 0f..1f) { "minVoiceConfidence must be in [0, 1]" }
        require(minSoundSmoothedEnergy > 0.0) { "minSoundSmoothedEnergy must be > 0" }
        require(minSoundPeakEnergy > 0.0) { "minSoundPeakEnergy must be > 0" }
        require(minKeywordConfidence in 0f..1f) { "minKeywordConfidence must be in [0, 1]" }
    }

    fun evaluate(stimulus: AudioStimulus): MeaningfulAudioStimulus? {
        if (stimulus.timestampMs <= 0L) {
            return null
        }
        return when (stimulus) {
            is VoiceActivityStimulus -> evaluateVoiceStimulus(stimulus)
            is SoundStimulus -> evaluateSoundStimulus(stimulus)
            is KeywordStimulus -> evaluateKeywordStimulus(stimulus)
        }
    }

    private fun evaluateVoiceStimulus(stimulus: VoiceActivityStimulus): MeaningfulAudioStimulus? {
        val confidence = stimulus.confidence
        if (stimulus.state != VoiceActivityStimulusState.STARTED) {
            return null
        }
        if (!confidence.isFinite() || confidence !in 0f..1f) {
            return null
        }
        if (confidence < minVoiceConfidence) {
            return null
        }
        return MeaningfulAudioStimulus(
            timestampMs = stimulus.timestampMs,
            sourceEventType = stimulus.sourceEventType,
            reason = "VOICE_STARTED(confidence=$confidence)"
        )
    }

    private fun evaluateSoundStimulus(stimulus: SoundStimulus): MeaningfulAudioStimulus? {
        val smoothedEnergy = stimulus.smoothedEnergy
        val peakEnergy = stimulus.peak
        if (!smoothedEnergy.isFinite() || !peakEnergy.isFinite()) {
            return null
        }
        val isStrongSound = smoothedEnergy >= minSoundSmoothedEnergy ||
            peakEnergy >= minSoundPeakEnergy
        if (!isStrongSound) {
            return null
        }
        return MeaningfulAudioStimulus(
            timestampMs = stimulus.timestampMs,
            sourceEventType = stimulus.sourceEventType,
            reason = "STRONG_SOUND(kind=${stimulus.kind},smoothed=$smoothedEnergy,peak=$peakEnergy)"
        )
    }

    private fun evaluateKeywordStimulus(stimulus: KeywordStimulus): MeaningfulAudioStimulus? {
        val confidence = stimulus.confidence
        if (!confidence.isFinite() || confidence !in 0f..1f) {
            return null
        }
        if (confidence < minKeywordConfidence) {
            return null
        }
        return MeaningfulAudioStimulus(
            timestampMs = stimulus.timestampMs,
            sourceEventType = stimulus.sourceEventType,
            reason = "KEYWORD_DETECTED(kind=${stimulus.kind.name},id=${stimulus.keywordId},confidence=$confidence)"
        )
    }

    companion object {
        private const val DEFAULT_MIN_VOICE_CONFIDENCE = 0.03f
        private const val DEFAULT_MIN_SOUND_SMOOTHED_ENERGY = 0.25
        private const val DEFAULT_MIN_SOUND_PEAK_ENERGY = 0.45
        private const val DEFAULT_MIN_KEYWORD_CONFIDENCE = 0.03f
    }
}

data class MeaningfulAudioStimulus(
    val timestampMs: Long,
    val sourceEventType: EventType,
    val reason: String
)
