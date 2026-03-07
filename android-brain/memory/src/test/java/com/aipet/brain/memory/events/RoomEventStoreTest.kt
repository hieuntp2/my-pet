package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.memory.db.EventDao
import com.aipet.brain.memory.db.EventEntity
import com.aipet.brain.memory.db.EventEntityForExport
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

    @Test
    fun listForExportPage_usesStableRowIdCursorSnapshotWithoutDuplicates() = runTest {
        val fakeDao = FakeEventDao()
        val store = RoomEventStore(fakeDao)
        fakeDao.seed(
            EventEntity(
                eventId = "event-1",
                type = EventType.TEST_EVENT.name,
                timestampMs = 1_000L,
                payloadJson = "{}",
                schemaVersion = 1
            )
        )
        fakeDao.seed(
            EventEntity(
                eventId = "event-2",
                type = EventType.TEST_EVENT.name,
                timestampMs = 2_000L,
                payloadJson = "{}",
                schemaVersion = 1
            )
        )
        fakeDao.seed(
            EventEntity(
                eventId = "event-3",
                type = EventType.TEST_EVENT.name,
                timestampMs = 3_000L,
                payloadJson = "{}",
                schemaVersion = 1
            )
        )
        val snapshotCursor = store.latestExportCursor()
            ?: error("Expected snapshot cursor")

        fakeDao.seed(
            EventEntity(
                eventId = "event-new-after-snapshot",
                type = EventType.TEST_EVENT.name,
                timestampMs = 9_000L,
                payloadJson = "{}",
                schemaVersion = 1
            )
        )

        val firstPage = store.listForExportPage(
            limit = 2,
            snapshotCursor = snapshotCursor,
            afterCursorExclusive = null
        )
        val secondPage = store.listForExportPage(
            limit = 2,
            snapshotCursor = snapshotCursor,
            afterCursorExclusive = firstPage.last().cursor
        )
        val exportedIds = (firstPage + secondPage).map { it.event.eventId }

        assertEquals(listOf("event-3", "event-2", "event-1"), exportedIds)
    }
}

private class FakeEventDao : EventDao {
    private val rows = mutableListOf<StoredEventRow>()
    private val flow = MutableStateFlow<List<EventEntity>>(emptyList())
    private var nextRowId = 1L

    override suspend fun insert(event: EventEntity) {
        rows.removeAll { it.event.eventId == event.eventId }
        rows.add(
            StoredEventRow(
                rowId = nextRowId++,
                event = event
            )
        )
        publish()
    }

    override suspend fun listLatest(limit: Int): List<EventEntity> {
        return sortedEvents().take(limit)
    }

    override suspend fun listForExportPage(
        limit: Int,
        snapshotRowId: Long,
        afterRowIdExclusive: Long?
    ): List<EventEntityForExport> {
        return rows.filter { row ->
            row.rowId <= snapshotRowId &&
                (afterRowIdExclusive == null || row.rowId < afterRowIdExclusive)
        }.sortedByDescending { it.rowId }
            .take(limit)
            .map { row ->
                EventEntityForExport(
                    rowId = row.rowId,
                    eventId = row.event.eventId,
                    type = row.event.type,
                    timestampMs = row.event.timestampMs,
                    payloadJson = row.event.payloadJson,
                    schemaVersion = row.event.schemaVersion
                )
            }
    }

    override suspend fun latestForExportCursor(): EventEntityForExport? {
        val latest = rows.maxByOrNull { it.rowId } ?: return null
        return EventEntityForExport(
            rowId = latest.rowId,
            eventId = latest.event.eventId,
            type = latest.event.type,
            timestampMs = latest.event.timestampMs,
            payloadJson = latest.event.payloadJson,
            schemaVersion = latest.event.schemaVersion
        )
    }

    override fun observeLatest(limit: Int): Flow<List<EventEntity>> {
        return flow.map { list ->
            list.sortedWith(
                compareByDescending<EventEntity> { it.timestampMs }
                    .thenByDescending { it.eventId }
            ).take(limit)
        }
    }

    override suspend fun clearAll() {
        rows.clear()
        publish()
    }

    fun seed(event: EventEntity) {
        rows.add(
            StoredEventRow(
                rowId = nextRowId++,
                event = event
            )
        )
        publish()
    }

    private fun publish() {
        flow.value = rows.map { it.event }
    }

    private fun sortedEvents(): List<EventEntity> {
        return rows.map { it.event }.sortedWith(
            compareByDescending<EventEntity> { it.timestampMs }
                .thenByDescending { it.eventId }
        )
    }
}

private data class StoredEventRow(
    val rowId: Long,
    val event: EventEntity
)
