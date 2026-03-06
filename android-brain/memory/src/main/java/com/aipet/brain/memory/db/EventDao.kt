package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: EventEntity)

    @Query("SELECT * FROM events ORDER BY timestamp_ms DESC, event_id DESC LIMIT :limit")
    suspend fun listLatest(limit: Int): List<EventEntity>

    @Query("SELECT * FROM events ORDER BY timestamp_ms DESC, event_id DESC LIMIT :limit")
    fun observeLatest(limit: Int): Flow<List<EventEntity>>

    @Query("DELETE FROM events")
    suspend fun clearAll()
}
