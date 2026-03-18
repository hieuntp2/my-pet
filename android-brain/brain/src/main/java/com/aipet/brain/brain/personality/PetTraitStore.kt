package com.aipet.brain.brain.personality

import kotlinx.coroutines.flow.Flow

interface PetTraitStore {
    suspend fun getByPetId(petId: String): PetTrait?

    fun observeByPetId(petId: String): Flow<PetTrait?>

    suspend fun upsert(trait: PetTrait)
}
