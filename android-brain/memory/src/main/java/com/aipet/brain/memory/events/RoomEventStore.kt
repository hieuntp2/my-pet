package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.memory.db.EventDao
import com.aipet.brain.memory.db.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomEventStore(
    private val eventDao: EventDao
) : EventStore {
    override suspend fun save(event: EventEnvelope) {
        eventDao.insert(event.toEntity())
    }

    override suspend fun listLatest(limit: Int): List<EventEnvelope> {
        return eventDao.listLatest(limit).map { it.toEnvelope() }
    }

    override fun observeLatest(limit: Int): Flow<List<EventEnvelope>> {
        return eventDao.observeLatest(limit).map { entities ->
            entities.map { entity -> entity.toEnvelope() }
        }
    }

    override suspend fun clearAll() {
        eventDao.clearAll()
    }

    private fun EventEnvelope.toEntity(): EventEntity {
        return EventEntity(
            eventId = eventId,
            type = type.name,
            timestampMs = timestampMs,
            payloadJson = EventEnvelope.normalizePayloadJson(payloadJson),
            schemaVersion = EventEnvelope.normalizeSchemaVersion(schemaVersion)
        )
    }

    private fun EventEntity.toEnvelope(): EventEnvelope {
        return EventEnvelope(
            eventId = eventId,
            type = EventType.fromRawValue(type),
            timestampMs = timestampMs,
            payloadJson = EventEnvelope.normalizePayloadJson(payloadJson),
            schemaVersion = EventEnvelope.normalizeSchemaVersion(schemaVersion)
        )
    }
}
