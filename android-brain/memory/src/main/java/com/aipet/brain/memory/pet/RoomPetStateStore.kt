package com.aipet.brain.memory.pet

import com.aipet.brain.brain.pet.PetState
import com.aipet.brain.brain.pet.PetStateStore

class RoomPetStateStore(
    private val dao: PetStateDao
) : PetStateStore {
    override suspend fun getCurrentState(): PetState? {
        return dao.getCurrentState()?.toDomain()
    }

    override suspend fun upsertState(state: PetState) {
        dao.upsertState(PetStateEntity.fromDomain(state))
    }
}
