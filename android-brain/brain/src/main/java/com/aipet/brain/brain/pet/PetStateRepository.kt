package com.aipet.brain.brain.pet

class PetStateRepository(
    private val store: PetStateStore,
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun getOrCreateState(): PetState {
        val existing = store.getCurrentState()
        if (existing != null) {
            return existing
        }

        val initial = defaultState(createdAt = clock())
        store.upsertState(initial)
        return initial
    }

    suspend fun updateState(state: PetState): PetState {
        val normalized = state.withClampedValues(lastUpdatedAt = state.lastUpdatedAt)
        store.upsertState(normalized)
        return normalized
    }

    private fun defaultState(createdAt: Long): PetState {
        return PetState(
            mood = PetMood.NEUTRAL,
            energy = 70,
            hunger = 30,
            sleepiness = 20,
            social = 50,
            bond = 0,
            lastUpdatedAt = createdAt
        )
    }
}
