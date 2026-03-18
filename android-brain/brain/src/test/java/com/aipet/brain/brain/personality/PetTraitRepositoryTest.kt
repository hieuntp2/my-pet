package com.aipet.brain.brain.personality

import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Test

class PetTraitRepositoryTest {
    @Test
    fun `getOrCreateForPet creates default traits when missing`() = runBlocking {
        val store = InMemoryPetTraitStore()
        val repository = PetTraitRepository(
            store = store,
            clock = { 1234L }
        )

        val traits = repository.getOrCreateForPet("pet-1")

        assertEquals("pet-1", traits.petId)
        assertEquals(PetTraitRepository.DEFAULT_PLAYFUL, traits.playful)
        assertEquals(PetTraitRepository.DEFAULT_LAZY, traits.lazy)
        assertEquals(PetTraitRepository.DEFAULT_CURIOUS, traits.curious)
        assertEquals(PetTraitRepository.DEFAULT_SOCIAL, traits.social)
        assertEquals(1234L, traits.updatedAt)
        assertEquals(traits, store.getByPetId("pet-1"))
    }

    @Test
    fun `getOrCreateForPet reuses persisted traits`() = runBlocking {
        val existing = PetTrait(
            petId = "pet-1",
            playful = 0.7f,
            lazy = 0.2f,
            curious = 0.8f,
            social = 0.9f,
            updatedAt = 3000L
        )
        val store = InMemoryPetTraitStore(existing)
        val repository = PetTraitRepository(store = store)

        val traits = repository.getOrCreateForPet("pet-1")

        assertSame(existing, traits)
    }

    @Test
    fun `save persists provided trait values`() = runBlocking {
        val store = InMemoryPetTraitStore()
        val repository = PetTraitRepository(store = store)
        val updated = PetTrait(
            petId = "pet-1",
            playful = 0.8f,
            lazy = 0.1f,
            curious = 0.9f,
            social = 0.75f,
            updatedAt = 4567L
        )

        val saved = repository.save(updated)

        assertEquals(updated, saved)
        assertEquals(updated, store.getByPetId("pet-1"))
    }
}

private class InMemoryPetTraitStore(
    initialTrait: PetTrait? = null
) : PetTraitStore {
    private val traitsByPetId = mutableMapOf<String, PetTrait>()
    private val traitFlowsByPetId = mutableMapOf<String, MutableStateFlow<PetTrait?>>()

    init {
        if (initialTrait != null) {
            traitsByPetId[initialTrait.petId] = initialTrait
            traitFlowsByPetId[initialTrait.petId] = MutableStateFlow(initialTrait)
        }
    }

    override suspend fun getByPetId(petId: String): PetTrait? {
        return traitsByPetId[petId]
    }

    override fun observeByPetId(petId: String): Flow<PetTrait?> {
        return traitFlowsByPetId.getOrPut(petId) {
            MutableStateFlow(traitsByPetId[petId])
        }
    }

    override suspend fun upsert(trait: PetTrait) {
        traitsByPetId[trait.petId] = trait
        traitFlowsByPetId.getOrPut(trait.petId) {
            MutableStateFlow(trait)
        }.value = trait
    }
}
