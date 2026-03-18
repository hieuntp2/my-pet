package com.aipet.brain.memory.pet

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface PetStateDao {
    @Query("SELECT * FROM pet_state WHERE id = :id LIMIT 1")
    suspend fun getCurrentState(id: Int = PetStateEntity.SINGLETON_ID): PetStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertState(state: PetStateEntity)
}
