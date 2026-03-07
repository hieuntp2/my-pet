package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.memory.db.EventDao
import com.aipet.brain.memory.db.EventEntityForExport

internal class RoomEventExportTraversal(
    private val eventDao: EventDao
) {
    /**
     * Export traversal currently relies on SQLite rowid monotonic insertion behavior.
     * Keep this assumption localized so future schema/storage changes only touch this layer.
     */
    suspend fun latestSnapshotCursor(): ExportCursor? {
        return eventDao.latestForExportCursor()?.toCursor()
    }

    suspend fun listPage(
        limit: Int,
        snapshotCursor: ExportCursor,
        afterCursorExclusive: ExportCursor?
    ): List<ExportEventRecord> {
        return eventDao.listForExportPage(
            limit = limit,
            snapshotRowId = snapshotCursor.rowId,
            afterRowIdExclusive = afterCursorExclusive?.rowId
        ).map { row ->
            ExportEventRecord(
                cursor = row.toCursor(),
                event = row.toEnvelope()
            )
        }
    }

    private fun EventEntityForExport.toCursor(): ExportCursor {
        return ExportCursor(rowId = rowId)
    }

    private fun EventEntityForExport.toEnvelope(): EventEnvelope {
        return EventEnvelope(
            eventId = eventId,
            type = EventType.fromRawValue(type),
            timestampMs = timestampMs,
            payloadJson = EventEnvelope.normalizePayloadJson(payloadJson),
            schemaVersion = EventEnvelope.normalizeSchemaVersion(schemaVersion)
        )
    }
}
