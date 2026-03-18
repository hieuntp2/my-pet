package com.aipet.brain.brain.personality

import kotlinx.coroutines.flow.Flow

class PetTraitRepository(
    private val store: PetTraitStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun getOrCreateForPet(petId: String): PetTrait {
        val normalizedPetId = petId.trim()
        require(normalizedPetId.isNotBlank()) { "petId cannot be blank." }

        val existing = store.getByPetId(normalizedPetId)
        if (existing != null) {
            return existing
        }

        val created = defaultTraits(normalizedPetId)
        store.upsert(created)
        return created
    }

    suspend fun getForPet(petId: String): PetTrait? {
        val normalizedPetId = petId.trim()
        require(normalizedPetId.isNotBlank()) { "petId cannot be blank." }
        return store.getByPetId(normalizedPetId)
    }

    fun observeForPet(petId: String): Flow<PetTrait?> {
        val normalizedPetId = petId.trim()
        require(normalizedPetId.isNotBlank()) { "petId cannot be blank." }
        return store.observeByPetId(normalizedPetId)
    }

    suspend fun save(trait: PetTrait): PetTrait {
        store.upsert(trait)
        return trait
    }

    private fun defaultTraits(petId: String): PetTrait {
        return PetTrait(
            petId = petId,
            playful = DEFAULT_PLAYFUL,
            lazy = DEFAULT_LAZY,
            curious = DEFAULT_CURIOUS,
            social = DEFAULT_SOCIAL,
            updatedAt = clock()
        )
    }

    companion object {
        const val DEFAULT_PLAYFUL: Float = 0.55f
        const val DEFAULT_LAZY: Float = 0.35f
        const val DEFAULT_CURIOUS: Float = 0.60f
        const val DEFAULT_SOCIAL: Float = 0.50f
    }
}
