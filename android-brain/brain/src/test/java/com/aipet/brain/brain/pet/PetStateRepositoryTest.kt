package com.aipet.brain.brain.pet

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class PetStateRepositoryTest {
    @Test
    fun `getOrCreateState creates deterministic default state on first load`() = runTest {
        val store = FakePetStateStore()
        val repository = PetStateRepository(
            store = store,
            clock = { 5_000L }
        )

        val state = repository.getOrCreateState()

        assertEquals(PetMood.NEUTRAL, state.mood)
        assertEquals(70, state.energy)
        assertEquals(30, state.hunger)
        assertEquals(50, state.social)
        assertEquals(0, state.bond)
        assertEquals(20, state.sleepiness)
        assertEquals(5_000L, state.lastUpdatedAt)
        assertEquals(state, store.currentState)
    }

    @Test
    fun `getOrCreateState reuses existing state`() = runTest {
        val existing = PetState(
            mood = PetMood.HAPPY,
            energy = 60,
            hunger = 20,
            sleepiness = 25,
            social = 55,
            bond = 10,
            lastUpdatedAt = 2_000L
        )
        val store = FakePetStateStore(existing)
        val repository = PetStateRepository(store = store)

        val state = repository.getOrCreateState()

        assertSame(existing, state)
    }

    @Test
    fun `updateState preserves supplied timestamp`() = runTest {
        val store = FakePetStateStore()
        val repository = PetStateRepository(
            store = store,
            clock = { 99_999L }
        )

        val updated = repository.updateState(
            PetState(
                mood = PetMood.EXCITED,
                energy = 100,
                hunger = 0,
                sleepiness = 40,
                social = 80,
                bond = 20,
                lastUpdatedAt = 8_500L
            )
        )

        assertEquals(8_500L, updated.lastUpdatedAt)
        assertEquals(100, updated.energy)
        assertEquals(0, updated.hunger)
        assertEquals(80, updated.social)
        assertEquals(20, updated.bond)
    }

    private class FakePetStateStore(
        initialState: PetState? = null
    ) : PetStateStore {
        var currentState: PetState? = initialState

        override suspend fun getCurrentState(): PetState? = currentState

        override suspend fun upsertState(state: PetState) {
            currentState = state
        }
    }
}
