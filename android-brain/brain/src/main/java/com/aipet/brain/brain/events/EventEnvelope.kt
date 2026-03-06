package com.aipet.brain.brain.events

import java.util.UUID

data class EventEnvelope(
    val eventId: String,
    val type: EventType,
    val timestampMs: Long,
    val payloadJson: String
) {
    companion object {
        fun create(
            type: EventType,
            payloadJson: String = "{}",
            timestampMs: Long = System.currentTimeMillis(),
            eventId: String = UUID.randomUUID().toString()
        ): EventEnvelope {
            return EventEnvelope(
                eventId = eventId,
                type = type,
                timestampMs = timestampMs,
                payloadJson = payloadJson
            )
        }
    }
}
