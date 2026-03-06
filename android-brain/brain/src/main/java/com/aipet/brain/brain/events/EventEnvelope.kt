package com.aipet.brain.brain.events

import java.util.UUID

data class EventEnvelope(
    val eventId: String,
    val type: EventType,
    val timestampMs: Long,
    val payloadJson: String,
    val schemaVersion: Int = CURRENT_SCHEMA_VERSION
) {
    companion object {
        const val CURRENT_SCHEMA_VERSION: Int = 1

        fun create(
            type: EventType,
            payloadJson: String = "{}",
            timestampMs: Long = System.currentTimeMillis(),
            eventId: String = UUID.randomUUID().toString(),
            schemaVersion: Int = CURRENT_SCHEMA_VERSION
        ): EventEnvelope {
            return EventEnvelope(
                eventId = eventId,
                type = type,
                timestampMs = timestampMs,
                payloadJson = normalizePayloadJson(payloadJson),
                schemaVersion = normalizeSchemaVersion(schemaVersion)
            )
        }

        fun normalizePayloadJson(payloadJson: String): String {
            return payloadJson.takeIf { it.isNotBlank() } ?: "{}"
        }

        fun normalizeSchemaVersion(schemaVersion: Int): Int {
            return schemaVersion.takeIf { it > 0 } ?: CURRENT_SCHEMA_VERSION
        }
    }
}
