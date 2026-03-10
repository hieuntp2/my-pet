package com.aipet.brain.brain.logic.audio

import com.aipet.brain.brain.events.EventType
import java.util.Locale

sealed interface AudioStimulus {
    val timestampMs: Long
    val sourceEventType: EventType
}

data class SoundStimulus(
    override val timestampMs: Long,
    override val sourceEventType: EventType,
    val rms: Double,
    val peak: Double,
    val smoothedEnergy: Double,
    val kind: String
) : AudioStimulus

enum class VoiceActivityStimulusState {
    STARTED,
    ENDED
}

data class VoiceActivityStimulus(
    override val timestampMs: Long,
    override val sourceEventType: EventType,
    val state: VoiceActivityStimulusState,
    val confidence: Float,
    val vadState: String?
) : AudioStimulus

enum class KeywordStimulusKind {
    WAKE_WORD,
    KEYWORD
}

data class KeywordStimulus(
    override val timestampMs: Long,
    override val sourceEventType: EventType,
    val kind: KeywordStimulusKind,
    val keywordId: String,
    val keywordText: String?,
    val confidence: Float,
    val engine: String
) : AudioStimulus

fun AudioStimulus.toDebugSummary(): String {
    return when (this) {
        is SoundStimulus -> {
            "type=SOUND kind=$kind smoothed=${"%.4f".format(Locale.US, smoothedEnergy)} " +
                "rms=${"%.4f".format(Locale.US, rms)} peak=${"%.4f".format(Locale.US, peak)} " +
                "source=${sourceEventType.name} ts=$timestampMs"
        }

        is VoiceActivityStimulus -> {
            "type=VOICE state=${state.name} confidence=${"%.2f".format(Locale.US, confidence)} " +
                "vadState=${vadState ?: "-"} source=${sourceEventType.name} ts=$timestampMs"
        }

        is KeywordStimulus -> {
            "type=KEYWORD kind=${kind.name} id=$keywordId text=${keywordText ?: "-"} " +
                "confidence=${"%.3f".format(Locale.US, confidence)} engine=$engine " +
                "source=${sourceEventType.name} ts=$timestampMs"
        }
    }
}
