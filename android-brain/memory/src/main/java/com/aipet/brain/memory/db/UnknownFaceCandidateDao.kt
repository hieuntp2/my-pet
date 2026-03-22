package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface UnknownFaceCandidateDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(candidate: UnknownFaceCandidateEntity)

    @Query(
        """
        SELECT * FROM unknown_face_candidates
        WHERE candidate_id = :candidateId
        LIMIT 1
        """
    )
    suspend fun getById(candidateId: String): UnknownFaceCandidateEntity?

    @Query(
        """
        SELECT * FROM unknown_face_candidates
        WHERE status != :resolvedStatus
        ORDER BY last_seen_at_ms DESC, candidate_id ASC
        """
    )
    suspend fun listActive(resolvedStatus: String = "RESOLVED"): List<UnknownFaceCandidateEntity>

    @Query(
        """
        SELECT * FROM unknown_face_candidates
        ORDER BY last_seen_at_ms DESC, candidate_id ASC
        """
    )
    suspend fun listAll(): List<UnknownFaceCandidateEntity>

    @Query(
        """
        SELECT COUNT(*) FROM unknown_face_candidates
        WHERE status != :resolvedStatus
        """
    )
    suspend fun countActive(resolvedStatus: String = "RESOLVED"): Int

    @Query(
        """
        DELETE FROM unknown_face_candidates
        WHERE status = :resolvedStatus
            AND updated_at_ms < :olderThanMs
        """
    )
    suspend fun deleteResolvedOlderThan(
        resolvedStatus: String = "RESOLVED",
        olderThanMs: Long
    ): Int
}
