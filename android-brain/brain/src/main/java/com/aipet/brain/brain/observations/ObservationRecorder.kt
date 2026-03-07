package com.aipet.brain.brain.observations

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType

class ObservationRecorder(
    private val eventBus: EventBus
) {
    suspend fun recordObservation(observation: PerceptionObservation) {
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERCEPTION_OBSERVATION_RECORDED,
                payloadJson = observation.toPayloadJson(),
                timestampMs = observation.observedAtMs
            )
        )
    }

    suspend fun recordPersonLikeObservation(
        source: ObservationSource,
        note: String? = null,
        observedAtMs: Long = System.currentTimeMillis()
    ) {
        recordObservation(
            PerceptionObservation.create(
                source = source,
                observationType = ObservationType.PERSON_LIKE,
                note = note,
                observedAtMs = observedAtMs
            )
        )
    }
}
