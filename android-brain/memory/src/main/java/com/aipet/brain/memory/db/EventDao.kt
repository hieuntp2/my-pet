package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.ColumnInfo
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY timestamp_ms DESC, event_id DESC LIMIT :limit")
    suspend fun listLatest(limit: Int): List<EventEntity>

    @Query(
        """
        SELECT
            rowid AS row_id,
            event_id,
            type,
            timestamp_ms,
            payload_json,
            schema_version
        FROM events
        WHERE rowid <= :snapshotRowId
          AND (:afterRowIdExclusive IS NULL OR rowid < :afterRowIdExclusive)
        ORDER BY rowid DESC
        LIMIT :limit
        """
    )
    suspend fun listForExportPage(
        limit: Int,
        snapshotRowId: Long,
        afterRowIdExclusive: Long?
    ): List<EventEntityForExport>

    @Query(
        """
        SELECT
            rowid AS row_id,
            event_id,
            type,
            timestamp_ms,
            payload_json,
            schema_version
        FROM events
        ORDER BY rowid DESC
        LIMIT 1
        """
    )
    suspend fun latestForExportCursor(): EventEntityForExport?

    @Query("SELECT * FROM events ORDER BY timestamp_ms DESC, event_id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<EventEntity>>

    @Query("DELETE FROM events")
    suspend fun clearAll()
}

data class EventEntityForExport(
    @ColumnInfo(name = "row_id")
    val rowId: Long,
    @ColumnInfo(name = "event_id")
    val eventId: String,
    @ColumnInfo(name = "type")
    val type: String,
    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long,
    @ColumnInfo(name = "payload_json")
    val payloadJson: String,
    @ColumnInfo(name = "schema_version")
    val schemaVersion: Int
)
