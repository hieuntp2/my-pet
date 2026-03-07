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

    @Query("SELECT * FROM persons ORDER BY updated_at_ms DESC, person_id ASC")
    suspend fun listAll(): List<PersonEntity>

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

    @Transaction
    suspend fun assignOwner(personId: String, updatedAtMs: Long): Boolean {
        clearOwnerFlags(updatedAtMs)
        return setOwnerById(personId = personId, updatedAtMs = updatedAtMs) > 0
    }
}
