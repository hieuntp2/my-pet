package com.aipet.brain.brain.logic.audio

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType

/**
 * Shared gate that normalizes audio-event mapping and meaningful-stimulus checks.
 *
 * Both brain state wake logic and working-memory updates depend on the same conservative
 * criteria, so this keeps event parsing and policy decisions consistent.
 */
class MeaningfulAudioStimulusGate(
    private val audioStimulusMapper: AudioStimulusMapper = AudioStimulusMapper(),
    private val audioMeaningfulStimulusPolicy: AudioMeaningfulStimulusPolicy = AudioMeaningfulStimulusPolicy()
) {
    fun evaluate(event: EventEnvelope): MeaningfulAudioStimulusGateResult {
        val mappedStimulus = audioStimulusMapper.map(event)
            ?: return MeaningfulAudioStimulusGateResult(
                meaningfulStimulus = null,
                sourceEventType = event.type,
                timestampMs = event.timestampMs,
                rejectionReason = MeaningfulAudioStimulusRejectionReason.INVALID_PAYLOAD
            )

        val meaningfulStimulus = audioMeaningfulStimulusPolicy.evaluate(mappedStimulus)
            ?: return MeaningfulAudioStimulusGateResult(
                meaningfulStimulus = null,
                sourceEventType = mappedStimulus.sourceEventType,
                timestampMs = mappedStimulus.timestampMs,
                rejectionReason = MeaningfulAudioStimulusRejectionReason.POLICY_REJECTED
            )

        return MeaningfulAudioStimulusGateResult(
            meaningfulStimulus = meaningfulStimulus,
            sourceEventType = meaningfulStimulus.sourceEventType,
            timestampMs = meaningfulStimulus.timestampMs,
            rejectionReason = null
        )
    }
}

data class MeaningfulAudioStimulusGateResult(
    val meaningfulStimulus: MeaningfulAudioStimulus?,
    val sourceEventType: EventType,
    val timestampMs: Long,
    val rejectionReason: MeaningfulAudioStimulusRejectionReason?
)

enum class MeaningfulAudioStimulusRejectionReason {
    INVALID_PAYLOAD,
    POLICY_REJECTED
}
