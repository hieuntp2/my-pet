package com.aipet.brain.memory.evolution

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface MemoryEpisodeDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: MemoryEpisodeEntity)

    @Query("SELECT * FROM memory_episodes ORDER BY created_at_ms DESC LIMIT :limit")
    suspend fun loadRecent(limit: Int): List<MemoryEpisodeEntity>

    @Query("SELECT * FROM memory_episodes WHERE created_at_ms >= :sinceMs ORDER BY created_at_ms DESC")
    suspend fun loadSince(sinceMs: Long): List<MemoryEpisodeEntity>

    @Query("SELECT * FROM memory_episodes WHERE importance_score >= :minImportance ORDER BY importance_score DESC LIMIT :limit")
    suspend fun loadByImportance(minImportance: Float, limit: Int): List<MemoryEpisodeEntity>

    @Query("SELECT COUNT(*) FROM memory_episodes")
    suspend fun count(): Int

    @Query("DELETE FROM memory_episodes WHERE created_at_ms < :olderThanMs")
    suspend fun deleteOlderThan(olderThanMs: Long)
}
