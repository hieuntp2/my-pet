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
            comfort = 70,
            stimulation = 30,
            moodValence = 10,
            moodArousal = 40,
            bond = 0,
            trustScore = 0,
            attachmentScore = 0,
            neglectStreak = 0,
            careStreak = 0,
            lastUpdatedAt = createdAt,
            lastOpenAt = 0L,
            lastMeaningfulInteractionAt = 0L
        )
    }
}
