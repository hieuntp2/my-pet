package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.memory.db.EventDao
import com.aipet.brain.memory.db.EventEntity
import com.aipet.brain.memory.db.EventEntityForExport
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RoomEventExportTraversalTest {
    @Test
    fun latestSnapshotCursor_returnsNullWhenNoRows() = runTest {
        val traversal = RoomEventExportTraversal(FakeTraversalEventDao())

        val cursor = traversal.latestSnapshotCursor()

        assertNull(cursor)
    }

    @Test
    fun listPage_usesSnapshotAndCursorWithoutDuplicates() = runTest {
        val fakeDao = FakeTraversalEventDao()
        val traversal = RoomEventExportTraversal(fakeDao)
        fakeDao.seed("event-1", 1_000L)
        fakeDao.seed("event-2", 2_000L)
        fakeDao.seed("event-3", 3_000L)
        val snapshotCursor = traversal.latestSnapshotCursor()
            ?: error("Expected snapshot cursor")

        fakeDao.seed("event-new-after-snapshot", 9_000L)

        val firstPage = traversal.listPage(
            limit = 2,
            snapshotCursor = snapshotCursor,
            afterCursorExclusive = null
        )
        val secondPage = traversal.listPage(
            limit = 2,
            snapshotCursor = snapshotCursor,
            afterCursorExclusive = firstPage.last().cursor
        )
        val exportedIds = (firstPage + secondPage).map { it.event.eventId }

        assertEquals(listOf("event-3", "event-2", "event-1"), exportedIds)
    }
}

private class FakeTraversalEventDao : EventDao {
    private val rows = mutableListOf<TraversalStoredEventRow>()
    private val flow = MutableStateFlow<List<EventEntity>>(emptyList())
    private var nextRowId = 1L

    override suspend fun insert(event: EventEntity) {
        rows.removeAll { it.event.eventId == event.eventId }
        rows.add(
            TraversalStoredEventRow(
                rowId = nextRowId++,
                event = event
            )
        )
        publish()
    }

    override suspend fun listLatest(limit: Int): List<EventEntity> {
        return rows.map { it.event }.sortedWith(
            compareByDescending<EventEntity> { it.timestampMs }
                .thenByDescending { it.eventId }
        ).take(limit)
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
        return flow.map { events ->
            events.sortedWith(
                compareByDescending<EventEntity> { it.timestampMs }
                    .thenByDescending { it.eventId }
            ).take(limit)
        }
    }

    override suspend fun clearAll() {
        rows.clear()
        publish()
    }

    fun seed(
        eventId: String,
        timestampMs: Long
    ) {
        rows.add(
            TraversalStoredEventRow(
                rowId = nextRowId++,
                event = EventEntity(
                    eventId = eventId,
                    type = EventType.TEST_EVENT.name,
                    timestampMs = timestampMs,
                    payloadJson = "{}",
                    schemaVersion = 1
                )
            )
        )
        publish()
    }

    private fun publish() {
        flow.value = rows.map { it.event }
    }
}

private data class TraversalStoredEventRow(
    val rowId: Long,
    val event: EventEntity
)
