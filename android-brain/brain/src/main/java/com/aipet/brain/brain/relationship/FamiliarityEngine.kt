package com.aipet.brain.brain.relationship

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonRecognizedPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.events.RelationshipUpdatedEventPayload
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
                EventType.PERSON_RECOGNIZED -> {
                    val payload = PersonRecognizedPayload.fromJson(event.payloadJson) ?: return@collect
                    currentRecognizedPersonId = payload.personId
                    currentRecognizedAtMs = event.timestampMs
                    increaseFamiliarityAndPublishIfChanged(
                        personId = payload.personId,
                        delta = recognitionDelta,
                        updatedAtMs = event.timestampMs
                    )
                }

                EventType.PERSON_SEEN_RECORDED -> {
                    val payload = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return@collect
                    currentRecognizedPersonId = payload.personId
                    currentRecognizedAtMs = event.timestampMs
                    increaseFamiliarityAndPublishIfChanged(
                        personId = payload.personId,
                        delta = recognitionDelta,
                        updatedAtMs = event.timestampMs
                    )
                }

                EventType.USER_INTERACTED_PET,
                EventType.PET_LONG_PRESSED -> {
                    val currentPersonId = currentRecognizedPersonId ?: return@collect
                    val isWithinContextWindow = event.timestampMs - currentRecognizedAtMs <= contextTimeoutMs
                    if (!isWithinContextWindow) {
                        currentRecognizedPersonId = null
                        currentRecognizedAtMs = 0L
                        return@collect
                    }
                    increaseFamiliarityAndPublishIfChanged(
                        personId = currentPersonId,
                        delta = petDelta,
                        updatedAtMs = event.timestampMs
                    )
                }

                EventType.PERSON_UNKNOWN,
                EventType.PERSON_UNKNOWN_DETECTED -> {
                    currentRecognizedPersonId = null
                    currentRecognizedAtMs = 0L
                }

                else -> Unit
            }
        }
    }

    private suspend fun increaseFamiliarityAndPublishIfChanged(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ) {
        val result = familiarityStore.increaseFamiliarity(
            personId = personId,
            delta = delta,
            updatedAtMs = updatedAtMs
        ) ?: return
        if (result.previousFamiliarityScore == result.updatedFamiliarityScore) {
            return
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.RELATIONSHIP_UPDATED,
                timestampMs = result.updatedAtMs,
                payloadJson = RelationshipUpdatedEventPayload(
                    personId = result.personId,
                    familiarityScore = result.updatedFamiliarityScore,
                    updatedAtMs = result.updatedAtMs
                ).toJson()
            )
        )
    }

    companion object {
        const val DEFAULT_RECOGNITION_DELTA: Float = 0.05f
        const val DEFAULT_PET_DELTA: Float = 0.02f
        const val DEFAULT_CONTEXT_TIMEOUT_MS: Long = 30_000L
    }
}
