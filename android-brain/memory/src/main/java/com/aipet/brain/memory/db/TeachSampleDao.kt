package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachSampleDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(sample: TeachSampleEntity): Long

    @Query(
        """
        SELECT * FROM teach_samples
        WHERE session_id = :sessionId
        ORDER BY created_at_ms DESC, sample_id ASC
        LIMIT :limit
        """
    )
    suspend fun listBySession(sessionId: String, limit: Int): List<TeachSampleEntity>

    @Query(
        """
        SELECT * FROM teach_samples
        WHERE session_id = :sessionId
        ORDER BY created_at_ms DESC, sample_id ASC
        LIMIT :limit
        """
    )
    fun observeBySession(sessionId: String, limit: Int): Flow<List<TeachSampleEntity>>

    @Query(
        """
        DELETE FROM teach_samples
        WHERE session_id = :sessionId AND observation_id = :observationId
        """
    )
    suspend fun deleteBySessionAndObservation(sessionId: String, observationId: String): Int
}
