package com.aipet.brain.memory.pet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PetProfileDao {
    @Query("SELECT * FROM pet_profile WHERE slot_id = :slotId LIMIT 1")
    suspend fun getActiveProfile(slotId: Int = PetProfileEntity.SINGLETON_SLOT_ID): PetProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertActiveProfile(profile: PetProfileEntity)
}
