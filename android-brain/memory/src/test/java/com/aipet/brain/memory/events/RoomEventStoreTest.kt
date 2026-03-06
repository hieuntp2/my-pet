package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.memory.db.EventDao
import com.aipet.brain.memory.db.EventEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class RoomEventStoreTest {
    @Test
    fun listLatest_mapsUnknownTypeToFallbackWithoutCrash() = runTest {
        val fakeDao = FakeEventDao()
        fakeDao.seed(
            EventEntity(
                eventId = "event-1",
                type = "LEGACY_EVENT",
                timestampMs = 1_000L,
                payloadJson = "{\"legacy\":true}",
                schemaVersion = 1
            )
        )

        val store = RoomEventStore(fakeDao)

        val events = store.listLatest(limit = 10)

        assertEquals(1, events.size)
        assertEquals(EventType.UNKNOWN, events.first().type)
    }

    @Test
    fun save_normalizesBlankPayloadAndInvalidSchemaVersion() = runTest {
        val fakeDao = FakeEventDao()
        val store = RoomEventStore(fakeDao)

        store.save(
            EventEnvelope(
                eventId = "event-2",
                type = EventType.TEST_EVENT,
                timestampMs = 2_000L,
                payloadJson = "",
                schemaVersion = 0
            )
        )

        val saved = fakeDao.listLatest(limit = 10).first()

        assertEquals("{}", saved.payloadJson)
        assertEquals(EventEnvelope.CURRENT_SCHEMA_VERSION, saved.schemaVersion)
    }
}

private class FakeEventDao : EventDao {
    private val events = mutableListOf<EventEntity>()
    private val flow = MutableStateFlow<List<EventEntity>>(emptyList())

    override suspend fun insert(event: EventEntity) {
        events.removeAll { it.eventId == event.eventId }
        events.add(event)
        publish()
    }

    override suspend fun listLatest(limit: Int): List<EventEntity> {
        return events
            .sortedByDescending { it.timestampMs }
            .take(limit)
    }

    override fun observeLatest(limit: Int): Flow<List<EventEntity>> {
        return flow.map { list ->
            list.sortedByDescending { it.timestampMs }.take(limit)
        }
    }

    override suspend fun clearAll() {
        events.clear()
        publish()
    }

    fun seed(event: EventEntity) {
        events.add(event)
        publish()
    }

    private fun publish() {
        flow.value = events.toList()
    }
}
