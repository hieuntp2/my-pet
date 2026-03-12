package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TraitsSnapshotDao {
    @Insert
    suspend fun insert(snapshot: TraitsSnapshotEntity)

    @Query(
        """
        SELECT * FROM traits_snapshots
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    suspend fun getLatest(): TraitsSnapshotEntity?

    @Query(
        """
        SELECT * FROM traits_snapshots
        ORDER BY createdAt DESC
        LIMIT 1
        """
    )
    fun observeLatest(): Flow<TraitsSnapshotEntity?>
}
