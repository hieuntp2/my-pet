package com.aipet.brain.memory.personality

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PetTraitDao {
    @Query("SELECT * FROM pet_traits WHERE pet_id = :petId LIMIT 1")
    suspend fun getByPetId(petId: String): PetTraitEntity?

    @Query("SELECT * FROM pet_traits WHERE pet_id = :petId LIMIT 1")
    fun observeByPetId(petId: String): Flow<PetTraitEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: PetTraitEntity)
}
