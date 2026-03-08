package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TraitsSnapshotDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(snapshot: TraitsSnapshotEntity)

    @Query(
        """
        SELECT * FROM traits_snapshot
        ORDER BY captured_at_ms DESC, snapshot_id DESC
        LIMIT 1
        """
    )
    suspend fun latest(): TraitsSnapshotEntity?

    @Query(
        """
        SELECT * FROM traits_snapshot
        ORDER BY captured_at_ms DESC, snapshot_id DESC
        LIMIT 1
        """
    )
    fun observeLatest(): Flow<TraitsSnapshotEntity?>
}
