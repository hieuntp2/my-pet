package com.aipet.brain.brain.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetStateDecayEngineTest {
    private val engine = PetStateDecayEngine()

    @Test
    fun `applyDecay updates needs based on elapsed time and clamps values`() {
        val result = engine.applyDecay(
            currentState = PetState(
                mood = PetMood.NEUTRAL,
                energy = 2,
                hunger = 98,
                sleepiness = 96,
                social = 1,
                bond = 15,
                lastUpdatedAt = 1L
            ),
            now = 3_600_000L
        )

        assertEquals(0, result.energy)
        assertEquals(100, result.hunger)
        assertEquals(100, result.sleepiness)
        assertEquals(0, result.social)
        assertEquals(3_600_000L, result.lastUpdatedAt)
    }

    @Test
    fun `applyDecay returns current state when time does not advance`() {
        val state = PetState(
            mood = PetMood.NEUTRAL,
            energy = 70,
            hunger = 30,
            sleepiness = 20,
            social = 50,
            bond = 0,
            lastUpdatedAt = 10_000L
        )

        assertEquals(state, engine.applyDecay(state, now = 10_000L))
    }
}
