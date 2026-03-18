package com.aipet.brain.brain.pet

import java.util.UUID

class PetProfileRepository(
    private val store: PetProfileStore,
    private val clock: () -> Long = { System.currentTimeMillis() },
    private val idGenerator: () -> String = { UUID.randomUUID().toString() }
) {
    suspend fun getOrCreateActiveProfile(): PetProfile {
        val existing = store.getActiveProfile()
        if (existing != null) {
            return existing
        }

        val created = PetProfile(
            id = idGenerator(),
            name = DEFAULT_PET_NAME,
            createdAt = clock()
        )
        store.upsertActiveProfile(created)
        return created
    }

    suspend fun saveActiveProfile(profile: PetProfile): PetProfile {
        store.upsertActiveProfile(profile)
        return profile
    }

    companion object {
        const val DEFAULT_PET_NAME: String = "Buddy"
    }
}
