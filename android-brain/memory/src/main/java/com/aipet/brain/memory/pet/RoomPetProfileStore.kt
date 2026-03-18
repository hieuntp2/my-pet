package com.aipet.brain.memory.pet

import com.aipet.brain.brain.pet.PetProfile
import com.aipet.brain.brain.pet.PetProfileStore

class RoomPetProfileStore(
    private val dao: PetProfileDao
) : PetProfileStore {
    override suspend fun getActiveProfile(): PetProfile? {
        return dao.getActiveProfile()?.toDomain()
    }

    override suspend fun upsertActiveProfile(profile: PetProfile) {
        dao.upsertActiveProfile(PetProfileEntity.fromDomain(profile))
    }
}
