package com.aipet.brain.brain.pet

interface PetProfileStore {
    suspend fun getActiveProfile(): PetProfile?
    suspend fun upsertActiveProfile(profile: PetProfile)
}
