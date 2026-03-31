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
                comfort = 70,
                stimulation = 30,
                moodValence = 0,
                moodArousal = 50,
                bond = 15,
                trustScore = 0,
                attachmentScore = 0,
                neglectStreak = 0,
                careStreak = 0,
                lastUpdatedAt = 1L,
                lastOpenAt = 0L,
                lastMeaningfulInteractionAt = 0L
            ),
            now = 6_000_001L   // ~100 minutes: enough for energy(-3→0), hunger(+5→100), sleepiness(+4→100), social(-2→0)
        )

        assertEquals(0, result.energy)
        assertEquals(100, result.hunger)
        assertEquals(100, result.sleepiness)
        assertEquals(0, result.social)
        assertEquals(6_000_001L, result.lastUpdatedAt)
    }

    @Test
    fun `applyDecay returns current state when time does not advance`() {
        val state = PetState(
            mood = PetMood.NEUTRAL,
            energy = 70,
            hunger = 30,
            sleepiness = 20,
            social = 50,
            comfort = 70,
            stimulation = 30,
            moodValence = 0,
            moodArousal = 50,
            bond = 0,
            trustScore = 0,
            attachmentScore = 0,
            neglectStreak = 0,
            careStreak = 0,
            lastUpdatedAt = 10_000L,
            lastOpenAt = 0L,
            lastMeaningfulInteractionAt = 0L
        )

        assertEquals(state, engine.applyDecay(state, now = 10_000L))
    }
}
