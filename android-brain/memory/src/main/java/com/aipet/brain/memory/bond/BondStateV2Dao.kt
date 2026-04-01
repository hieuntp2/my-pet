package com.aipet.brain.memory.bond

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface BondStateV2Dao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: BondStateV2Entity)

    @Query("SELECT * FROM bond_state_v2 WHERE id = 'singleton' LIMIT 1")
    suspend fun load(): BondStateV2Entity?
}
