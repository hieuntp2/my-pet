package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import kotlinx.coroutines.flow.Flow

interface EventStore {
    suspend fun save(event: EventEnvelope)

    suspend fun listLatest(limit: Int): List<EventEnvelope>

    suspend fun latestExportCursor(): ExportCursor?

    suspend fun listForExportPage(
        limit: Int,
        snapshotCursor: ExportCursor,
        afterCursorExclusive: ExportCursor?
    ): List<ExportEventRecord>

    fun observeLatest(limit: Int): Flow<List<EventEnvelope>>

    suspend fun clearAll()
}

/**
 * Opaque traversal cursor for event export paging.
 * Backing semantics are internal to memory-layer export traversal.
 */
class ExportCursor internal constructor(
    internal val rowId: Long
)

data class ExportEventRecord(
    val cursor: ExportCursor,
    val event: EventEnvelope
)
