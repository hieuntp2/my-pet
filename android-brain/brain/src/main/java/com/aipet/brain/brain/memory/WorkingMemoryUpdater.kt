package com.aipet.brain.brain.memory

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.collect

class WorkingMemoryUpdater(
    private val eventBus: EventBus,
    private val workingMemoryStore: WorkingMemoryStore
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

                else -> Unit
            }
        }
    }
}
