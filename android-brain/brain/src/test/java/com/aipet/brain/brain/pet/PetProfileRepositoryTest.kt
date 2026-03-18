package com.aipet.brain.brain.pet

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PetProfileRepositoryTest {
    @Test
    fun `getOrCreateActiveProfile creates default profile when empty`() = runBlocking {
        val store = InMemoryPetProfileStore()
        val repository = PetProfileRepository(
            store = store,
            clock = { 123L },
            idGenerator = { "pet-1" }
        )

        val profile = repository.getOrCreateActiveProfile()

        assertEquals("pet-1", profile.id)
        assertEquals(PetProfileRepository.DEFAULT_PET_NAME, profile.name)
        assertEquals(123L, profile.createdAt)
        assertEquals(profile, store.current)
    }

    @Test
    fun `getOrCreateActiveProfile reuses existing profile`() = runBlocking {
        val existing = PetProfile(id = "pet-2", name = "Nori", createdAt = 999L)
        val store = InMemoryPetProfileStore(existing)
        val repository = PetProfileRepository(store)

        val profile = repository.getOrCreateActiveProfile()

        assertSame(existing, profile)
    }

    private class InMemoryPetProfileStore(
        initial: PetProfile? = null
    ) : PetProfileStore {
        var current: PetProfile? = initial

        override suspend fun getActiveProfile(): PetProfile? = current

        override suspend fun upsertActiveProfile(profile: PetProfile) {
            current = profile
        }
    }
}
