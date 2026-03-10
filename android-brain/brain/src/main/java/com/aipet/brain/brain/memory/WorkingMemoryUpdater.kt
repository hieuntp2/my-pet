package com.aipet.brain.brain.memory

import android.util.Log
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.logic.audio.AudioMeaningfulStimulusPolicy
import com.aipet.brain.brain.logic.audio.AudioStimulusMapper
import kotlinx.coroutines.flow.collect

class WorkingMemoryUpdater(
    private val eventBus: EventBus,
    private val workingMemoryStore: WorkingMemoryStore,
    private val audioStimulusMapper: AudioStimulusMapper = AudioStimulusMapper(),
    private val audioMeaningfulStimulusPolicy: AudioMeaningfulStimulusPolicy = AudioMeaningfulStimulusPolicy()
) {
    suspend fun observeEventsAndUpdateMemory() {
        eventBus.observe().collect { event ->
            when (event.type) {
                EventType.PERSON_SEEN_RECORDED -> {
                    val payload = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return@collect
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = payload.personId,
                            lastStimulusAtMs = event.timestampMs
                        )
                    }
                }

                EventType.PERSON_UNKNOWN_DETECTED -> {
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentPersonId = null,
                            lastStimulusAtMs = event.timestampMs
                        )
                    }
                }

                EventType.OBJECT_DETECTED -> {
                    val payload = ObjectDetectedEventPayload.fromJson(event.payloadJson) ?: return@collect
                    workingMemoryStore.update { current ->
                        current.copy(
                            currentObjectLabel = payload.label,
                            lastStimulusAtMs = event.timestampMs
                        )
                    }
                }

                EventType.USER_INTERACTED_PET -> {
                    workingMemoryStore.update { current ->
                        current.copy(lastStimulusAtMs = event.timestampMs)
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
        val stimulus = audioStimulusMapper.map(event) ?: run {
            Log.w(
                TAG,
                "Ignored audio event for working memory update due to invalid payload. " +
                    "eventType=${event.type.name}, eventId=${event.eventId}"
            )
            return
        }
        val meaningfulStimulus = audioMeaningfulStimulusPolicy.evaluate(stimulus)
        if (meaningfulStimulus == null) {
            Log.d(
                TAG,
                "Audio stimulus ignored for lastStimulusAtMs update. source=${stimulus.sourceEventType.name}, " +
                    "stimulusTs=${stimulus.timestampMs}"
            )
            return
        }
        val previousStimulusAtMs = workingMemoryStore.currentSnapshot().lastStimulusAtMs
        workingMemoryStore.update { current ->
            val currentTimestamp = current.lastStimulusAtMs
            if (currentTimestamp != null && meaningfulStimulus.timestampMs <= currentTimestamp) {
                current
            } else {
                current.copy(lastStimulusAtMs = meaningfulStimulus.timestampMs)
            }
        }
        val updatedStimulusAtMs = workingMemoryStore.currentSnapshot().lastStimulusAtMs
        Log.d(
            TAG,
            "Meaningful audio stimulus updated working memory. source=${meaningfulStimulus.sourceEventType.name}, " +
                "reason=${meaningfulStimulus.reason}, lastStimulusAtMs=$previousStimulusAtMs -> $updatedStimulusAtMs"
        )
    }

    companion object {
        private const val TAG = "WorkingMemoryUpdater"
    }
}
