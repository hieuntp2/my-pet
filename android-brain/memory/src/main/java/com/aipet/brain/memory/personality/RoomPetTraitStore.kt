package com.aipet.brain.memory.personality

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.personality.PetTraitStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomPetTraitStore(
    private val dao: PetTraitDao
) : PetTraitStore {
    override suspend fun getByPetId(petId: String): PetTrait? {
        return dao.getByPetId(petId)?.toDomain()
    }

    override fun observeByPetId(petId: String): Flow<PetTrait?> {
        return dao.observeByPetId(petId).map { entity ->
            entity?.toDomain()
        }
    }

    override suspend fun upsert(trait: PetTrait) {
        dao.upsert(PetTraitEntity.fromDomain(trait))
    }
}
