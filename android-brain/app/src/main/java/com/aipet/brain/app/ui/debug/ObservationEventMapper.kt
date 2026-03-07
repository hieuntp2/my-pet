package com.aipet.brain.app.ui.debug

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType

internal data class ObservationRecord(
    val observationId: String,
    val observedAtMs: Long,
    val source: String,
    val observationType: String,
    val note: String?
)

internal object ObservationEventMapper {
    fun listRecent(
        events: List<EventEnvelope>,
        limit: Int
    ): List<ObservationRecord> {
        return events.asSequence()
            .mapNotNull(::fromEvent)
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    fun fromEvent(event: EventEnvelope): ObservationRecord? {
        if (event.type != EventType.PERCEPTION_OBSERVATION_RECORDED) {
            return null
        }

        val parsedPayload = EventPayloadParser.parse(event.payloadJson)
        if (!parsedPayload.valid) {
            return null
        }

        val payloadObject = parsedPayload.value as? JsonObjectLiteral ?: return null
        val observationId = payloadObject.stringValue("observationId") ?: return null
        val source = payloadObject.stringValue("source") ?: return null
        val observationType = payloadObject.stringValue("observationType") ?: return null
        val observedAtMs = payloadObject.longValue("observedAtMs") ?: event.timestampMs
        val note = payloadObject.nullableStringValue("note")

        return ObservationRecord(
            observationId = observationId,
            observedAtMs = observedAtMs,
            source = source,
            observationType = observationType,
            note = note
        )
    }
}

private fun JsonObjectLiteral.stringValue(key: String): String? {
    return entries.firstOrNull { it.first == key }?.second as? String
}

private fun JsonObjectLiteral.nullableStringValue(key: String): String? {
    val value = entries.firstOrNull { it.first == key }?.second
    return when (value) {
        null -> null
        is String -> value
        else -> null
    }
}

private fun JsonObjectLiteral.longValue(key: String): Long? {
    val value = entries.firstOrNull { it.first == key }?.second ?: return null
    return when (value) {
        is JsonNumberLiteral -> value.raw.toLongOrNull()
        is Number -> value.toLong()
        is String -> value.toLongOrNull()
        else -> null
    }
}
