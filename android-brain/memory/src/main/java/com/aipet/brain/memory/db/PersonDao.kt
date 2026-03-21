package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

@Dao
interface PersonDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(person: PersonEntity)

    @Update
    suspend fun update(person: PersonEntity): Int

    @Query("SELECT * FROM persons WHERE person_id = :personId LIMIT 1")
    suspend fun getById(personId: String): PersonEntity?

    @Query(
        """
        SELECT * FROM persons
        ORDER BY familiarity_score DESC, updated_at_ms DESC, person_id ASC
        """
    )
    suspend fun listAll(): List<PersonEntity>

    @Query(
        """
        SELECT * FROM persons
        ORDER BY familiarity_score DESC, updated_at_ms DESC, person_id ASC
        LIMIT :limit
        """
    )
    suspend fun listTopByFamiliarity(limit: Int): List<PersonEntity>

    @Query("SELECT * FROM persons WHERE is_owner = 1 ORDER BY updated_at_ms DESC LIMIT 1")
    suspend fun getOwner(): PersonEntity?

    @Query("UPDATE persons SET is_owner = 0, updated_at_ms = :updatedAtMs WHERE is_owner = 1")
    suspend fun clearOwnerFlags(updatedAtMs: Long): Int

    @Query("UPDATE persons SET is_owner = 1, updated_at_ms = :updatedAtMs WHERE person_id = :personId")
    suspend fun setOwnerById(personId: String, updatedAtMs: Long): Int

    @Query(
        """
        UPDATE persons
        SET
            seen_count = seen_count + 1,
            last_seen_at_ms = CASE
                WHEN last_seen_at_ms IS NULL OR :seenAtMs >= last_seen_at_ms THEN :seenAtMs
                ELSE last_seen_at_ms
            END,
            updated_at_ms = :updatedAtMs
        WHERE person_id = :personId
        """
    )
    suspend fun incrementSeenCount(
        personId: String,
        seenAtMs: Long,
        updatedAtMs: Long
    ): Int

    @Query(
        """
        UPDATE persons
        SET
            familiarity_score = MIN(1.0, MAX(0.0, familiarity_score + :delta)),
            updated_at_ms = :updatedAtMs
        WHERE person_id = :personId
        """
    )
    suspend fun incrementFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): Int

    @Transaction
    suspend fun assignOwner(personId: String, updatedAtMs: Long): Boolean {
        clearOwnerFlags(updatedAtMs)
        return setOwnerById(personId = personId, updatedAtMs = updatedAtMs) > 0
    }

    @Query("DELETE FROM face_profiles WHERE linked_person_id = :personId")
    suspend fun deleteFaceProfilesByPersonId(personId: String): Int

    @Query("DELETE FROM persons WHERE person_id = :personId")
    suspend fun deleteById(personId: String): Int

    @Transaction
    suspend fun deletePersonAndRelatedData(personId: String): Boolean {
        deleteFaceProfilesByPersonId(personId)
        return deleteById(personId) > 0
    }
}
