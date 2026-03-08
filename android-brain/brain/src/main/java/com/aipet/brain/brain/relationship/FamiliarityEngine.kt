package com.aipet.brain.brain.relationship

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.collect

class FamiliarityEngine(
    private val eventBus: EventBus,
    private val familiarityStore: FamiliarityStore,
    private val recognitionDelta: Float = DEFAULT_RECOGNITION_DELTA,
    private val petDelta: Float = DEFAULT_PET_DELTA,
    private val contextTimeoutMs: Long = DEFAULT_CONTEXT_TIMEOUT_MS
) {
    private var currentRecognizedPersonId: String? = null
    private var currentRecognizedAtMs: Long = 0L

    suspend fun observeEventsAndApplyRules() {
        eventBus.observe().collect { event ->
            when (event.type) {
                EventType.PERSON_SEEN_RECORDED -> {
                    val payload = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return@collect
                    currentRecognizedPersonId = payload.personId
                    currentRecognizedAtMs = event.timestampMs
                    familiarityStore.increaseFamiliarity(
                        personId = payload.personId,
                        delta = recognitionDelta,
                        updatedAtMs = event.timestampMs
                    )
                }

                EventType.USER_INTERACTED_PET -> {
                    val currentPersonId = currentRecognizedPersonId ?: return@collect
                    val isWithinContextWindow = event.timestampMs - currentRecognizedAtMs <= contextTimeoutMs
                    if (!isWithinContextWindow) {
                        currentRecognizedPersonId = null
                        currentRecognizedAtMs = 0L
                        return@collect
                    }
                    familiarityStore.increaseFamiliarity(
                        personId = currentPersonId,
                        delta = petDelta,
                        updatedAtMs = event.timestampMs
                    )
                }

                EventType.PERSON_UNKNOWN_DETECTED -> {
                    currentRecognizedPersonId = null
                    currentRecognizedAtMs = 0L
                }

                else -> Unit
            }
        }
    }

    companion object {
        const val DEFAULT_RECOGNITION_DELTA: Float = 0.05f
        const val DEFAULT_PET_DELTA: Float = 0.02f
        const val DEFAULT_CONTEXT_TIMEOUT_MS: Long = 30_000L
    }
}
