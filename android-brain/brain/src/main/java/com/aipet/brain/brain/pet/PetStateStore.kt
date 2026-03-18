package com.aipet.brain.brain.pet

interface PetStateStore {
    suspend fun getCurrentState(): PetState?
    suspend fun upsertState(state: PetState)
}
