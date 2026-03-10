package com.aipet.brain.brain.logic.audio

import android.util.Log
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.KeywordDetectionPayload
import com.aipet.brain.brain.events.audio.SoundEnergyPayload
import com.aipet.brain.brain.events.audio.VoiceActivityPayload
import com.aipet.brain.brain.events.audio.VoiceActivityState

class AudioStimulusMapper {
    fun map(event: EventEnvelope): AudioStimulus? {
        return when (event.type) {
            EventType.SOUND_DETECTED -> mapSoundDetected(event)
            EventType.VOICE_ACTIVITY_STARTED -> mapVoiceActivity(
                event = event,
                expectedState = VoiceActivityStimulusState.STARTED
            )
            EventType.VOICE_ACTIVITY_ENDED -> mapVoiceActivity(
                event = event,
                expectedState = VoiceActivityStimulusState.ENDED
            )
            EventType.WAKE_WORD_DETECTED -> mapKeywordDetected(
                event = event,
                kind = KeywordStimulusKind.WAKE_WORD
            )
            EventType.KEYWORD_DETECTED -> mapKeywordDetected(
                event = event,
                kind = KeywordStimulusKind.KEYWORD
            )
            else -> null
        }
    }

    private fun mapSoundDetected(event: EventEnvelope): SoundStimulus? {
        val payload = SoundEnergyPayload.fromJson(event.payloadJson)
        if (payload == null) {
            Log.w(
                TAG,
                "Ignored ${event.type.name}: invalid payload. eventId=${event.eventId}, " +
                    "payload=${event.payloadJson}"
            )
            return null
        }

        return SoundStimulus(
            timestampMs = payload.timestamp.takeIf { it > 0L } ?: event.timestampMs,
            sourceEventType = event.type,
            rms = payload.rms,
            peak = payload.peak,
            smoothedEnergy = payload.smoothedEnergy,
            kind = payload.kind?.ifBlank { UNKNOWN_SOUND_KIND } ?: UNKNOWN_SOUND_KIND
        )
    }

    private fun mapVoiceActivity(
        event: EventEnvelope,
        expectedState: VoiceActivityStimulusState
    ): VoiceActivityStimulus? {
        val payload = VoiceActivityPayload.fromJson(event.payloadJson)
        if (payload == null) {
            Log.w(
                TAG,
                "Ignored ${event.type.name}: invalid payload. eventId=${event.eventId}, " +
                    "payload=${event.payloadJson}"
            )
            return null
        }

        val payloadState = payload.state.toStimulusState()
        if (payloadState != expectedState) {
            Log.w(
                TAG,
                "Mapped ${event.type.name} with mismatched payload state. " +
                    "eventId=${event.eventId}, payloadState=${payload.state.name}, " +
                    "expectedState=${expectedState.name}"
            )
        }

        return VoiceActivityStimulus(
            timestampMs = payload.timestamp.takeIf { it > 0L } ?: event.timestampMs,
            sourceEventType = event.type,
            state = expectedState,
            confidence = payload.confidence,
            vadState = payload.vadState
        )
    }

    private fun mapKeywordDetected(
        event: EventEnvelope,
        kind: KeywordStimulusKind
    ): KeywordStimulus? {
        val payload = KeywordDetectionPayload.fromJson(event.payloadJson)
        if (payload == null) {
            Log.w(
                TAG,
                "Ignored ${event.type.name}: invalid payload. eventId=${event.eventId}, " +
                    "payload=${event.payloadJson}"
            )
            return null
        }

        if (!payload.confidence.isFinite() || payload.confidence !in 0f..1f) {
            Log.w(
                TAG,
                "Ignored ${event.type.name}: invalid confidence=${payload.confidence}. " +
                    "eventId=${event.eventId}"
            )
            return null
        }

        return KeywordStimulus(
            timestampMs = payload.timestamp.takeIf { it > 0L } ?: event.timestampMs,
            sourceEventType = event.type,
            kind = kind,
            keywordId = payload.keywordId,
            keywordText = payload.keywordText,
            confidence = payload.confidence,
            engine = payload.engine
        )
    }

    private fun VoiceActivityState.toStimulusState(): VoiceActivityStimulusState {
        return when (this) {
            VoiceActivityState.STARTED -> VoiceActivityStimulusState.STARTED
            VoiceActivityState.ENDED -> VoiceActivityStimulusState.ENDED
        }
    }

    companion object {
        private const val TAG = "AudioStimulusMapper"
        private const val UNKNOWN_SOUND_KIND = "UNKNOWN"
    }
}
