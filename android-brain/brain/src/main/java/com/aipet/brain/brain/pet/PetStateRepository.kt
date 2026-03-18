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

        val initial = PetState(
            mood = PetMood.NEUTRAL,
            energy = 70,
            hunger = 30,
            sleepiness = 20,
            social = 50,
            bond = 0,
            lastUpdatedAt = clock()
        )
        store.upsertState(initial)
        return initial
    }

    suspend fun updateState(state: PetState): PetState {
        val normalized = state.withClampedValues(lastUpdatedAt = clock())
        store.upsertState(normalized)
        return normalized
    }
}
