package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TeachSessionCompletionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(completion: TeachSessionCompletionEntity): Long

    @Query(
        """
        SELECT * FROM teach_session_completion
        WHERE teach_session_id = :teachSessionId
        LIMIT 1
        """
    )
    suspend fun getBySessionId(teachSessionId: String): TeachSessionCompletionEntity?

    @Query(
        """
        SELECT * FROM teach_session_completion
        WHERE teach_session_id = :teachSessionId
        LIMIT 1
        """
    )
    fun observeBySessionId(teachSessionId: String): Flow<TeachSessionCompletionEntity?>

    @Query(
        """
        DELETE FROM teach_session_completion
        WHERE teach_session_id = :teachSessionId
        """
    )
    suspend fun deleteBySessionId(teachSessionId: String): Int
}
