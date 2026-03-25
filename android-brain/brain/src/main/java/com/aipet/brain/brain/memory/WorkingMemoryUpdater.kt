package com.aipet.brain.brain.memory

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PersonRecognizedPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.logic.audio.AudioMeaningfulStimulusPolicy
import com.aipet.brain.brain.logic.audio.AudioStimulusMapper
import com.aipet.brain.brain.logic.audio.MeaningfulAudioStimulusGate
import com.aipet.brain.brain.logic.audio.MeaningfulAudioStimulusRejectionReason
import kotlinx.coroutines.flow.collect

class WorkingMemoryUpdater(
    private val eventBus: EventBus,
    private val workingMemoryStore: WorkingMemoryStore,
    private val audioStimulusMapper: AudioStimulusMapper = AudioStimulusMapper(),
    private val audioMeaningfulStimulusPolicy: AudioMeaningfulStimulusPolicy = AudioMeaningfulStimulusPolicy()
) {
    private val meaningfulAudioStimulusGate = MeaningfulAudioStimulusGate(
        audioStimulusMapper = audioStimulusMapper,
        audioMeaningfulStimulusPolicy = audioMeaningfulStimulusPolicy
    )

    suspend fun observeEventsAndUpdateMemory() {
        eventBus.observe().collect { event ->
            when (event.type) {
                EventType.PERSON_RECOGNIZED -> {
                    val payload = PersonRecognizedPayload.fromJson(event.payloadJson) ?: return@collect
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = payload.personId,
                            lastStimulusTs = event.timestampMs
                        )
                    }
                }

                EventType.PERSON_SEEN_RECORDED -> {
                    val payload = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return@collect
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = payload.personId,
                            lastStimulusTs = event.timestampMs
                        )
                    }
                }

                EventType.PERSON_UNKNOWN -> {
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = null,
                            lastStimulusTs = event.timestampMs
                        )
                    }
                }

                EventType.PERSON_UNKNOWN_DETECTED -> {
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = null,
                            lastStimulusTs = event.timestampMs
                        )
                    }
                }

                EventType.OBJECT_DETECTED -> {
                    val payload = ObjectDetectedEventPayload.fromJson(event.payloadJson) ?: return@collect
                    val resolvedObjectId = payload.objectId?.trim()?.ifBlank { null } ?: payload.label
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentObjectId = resolvedObjectId,
                            lastStimulusTs = event.timestampMs
                        )
                    }
                }

                EventType.USER_INTERACTED_PET,
                EventType.PET_LONG_PRESSED -> {
                    workingMemoryStore.update { current ->
                        current.copy(lastStimulusTs = event.timestampMs)
                    }
                }

                EventType.SOUND_DETECTED,
                EventType.VOICE_ACTIVITY_STARTED,
                EventType.VOICE_ACTIVITY_ENDED,
                EventType.WAKE_WORD_DETECTED,
                EventType.KEYWORD_DETECTED -> {
                    handleAudioStimulusMemoryUpdate(event)
                }

                else -> Unit
            }
        }
    }

    private fun handleAudioStimulusMemoryUpdate(event: EventEnvelope) {
        val gateResult = meaningfulAudioStimulusGate.evaluate(event)
        val meaningfulStimulus = gateResult.meaningfulStimulus
        if (meaningfulStimulus == null) {
            when (gateResult.rejectionReason) {
                MeaningfulAudioStimulusRejectionReason.INVALID_PAYLOAD -> {
                    Log.w(
                        TAG,
                        "Ignored audio event for working memory update due to invalid payload. " +
                            "eventType=${event.type.name}, eventId=${event.eventId}"
                    )
                }

                MeaningfulAudioStimulusRejectionReason.POLICY_REJECTED,
                null -> {
                    Log.d(
                        TAG,
                        "Audio stimulus ignored for lastStimulusTs update. source=${gateResult.sourceEventType.name}, " +
                            "stimulusTs=${gateResult.timestampMs}"
                    )
                }
            }
            return
        }
        val previousStimulusTs = workingMemoryStore.currentSnapshot().lastStimulusTs
        workingMemoryStore.update { current ->
            val currentTimestamp = current.lastStimulusTs
            if (currentTimestamp != null && meaningfulStimulus.timestampMs <= currentTimestamp) {
                current
            } else {
                current.copy(lastStimulusTs = meaningfulStimulus.timestampMs)
            }
        }
        val updatedStimulusTs = workingMemoryStore.currentSnapshot().lastStimulusTs
        Log.d(
            TAG,
            "Meaningful audio stimulus updated working memory. source=${meaningfulStimulus.sourceEventType.name}, " +
                "reason=${meaningfulStimulus.reason}, lastStimulusTs=$previousStimulusTs -> $updatedStimulusTs"
        )
    }

    companion object {
        private const val TAG = "WorkingMemoryUpdater"
    }
}
