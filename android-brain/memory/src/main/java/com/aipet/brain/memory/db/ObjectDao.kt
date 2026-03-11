package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertObject(entity: ObjectEntity)

    @Query("SELECT * FROM objects WHERE object_id = :objectId LIMIT 1")
    suspend fun getObjectById(objectId: String): ObjectEntity?

    @Query(
        """
        SELECT * FROM objects
        ORDER BY created_at_ms DESC, object_id ASC
        """
    )
    suspend fun getAllObjects(): List<ObjectEntity>

    @Query(
        """
        SELECT * FROM objects
        WHERE name = :name COLLATE NOCASE
        ORDER BY created_at_ms DESC, object_id ASC
        LIMIT 1
        """
    )
    suspend fun getLatestObjectByName(name: String): ObjectEntity?

    @Query(
        """
        UPDATE objects
        SET
            seen_count = seen_count + 1,
            last_seen_at_ms = CASE
                WHEN last_seen_at_ms IS NULL OR :seenAtMs >= last_seen_at_ms THEN :seenAtMs
                ELSE last_seen_at_ms
            END
        WHERE object_id = :objectId
        """
    )
    suspend fun incrementSeenStats(objectId: String, seenAtMs: Long): Int

    @Query("DELETE FROM objects WHERE object_id = :objectId")
    suspend fun deleteObject(objectId: String): Int
}
