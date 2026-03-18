package com.aipet.brain.brain.interaction

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetInteractionStateReducerTest {
    private val reducer = PetInteractionStateReducer()

    @Test
    fun `tap applies gentle social and bond increase`() {
        val updated = reducer.apply(
            currentState = state(social = 40, bond = 5, sleepiness = 20),
            interactionType = PetInteractionType.TAP,
            interactedAtMs = 2_000L
        )

        assertEquals(PetMood.HAPPY, updated.mood)
        assertEquals(41, updated.social)
        assertEquals(6, updated.bond)
        assertEquals(2_000L, updated.lastUpdatedAt)
    }

    @Test
    fun `long press applies stronger state effect than tap`() {
        val tapped = reducer.apply(
            currentState = state(social = 40, bond = 5, energy = 75),
            interactionType = PetInteractionType.TAP,
            interactedAtMs = 2_000L
        )
        val longPressed = reducer.apply(
            currentState = state(social = 40, bond = 5, energy = 75),
            interactionType = PetInteractionType.LONG_PRESS,
            interactedAtMs = 2_000L
        )

        assertEquals(PetMood.EXCITED, longPressed.mood)
        assertTrue(longPressed.social > tapped.social)
        assertTrue(longPressed.bond > tapped.bond)
    }

    @Test
    fun `sleepy long press stays bounded and avoids exaggerated jump`() {
        val updated = reducer.apply(
            currentState = state(social = 99, bond = 99, sleepiness = 85),
            interactionType = PetInteractionType.LONG_PRESS,
            interactedAtMs = 3_000L
        )

        assertEquals(PetMood.CURIOUS, updated.mood)
        assertEquals(100, updated.social)
        assertEquals(100, updated.bond)
    }

    private fun state(
        mood: PetMood = PetMood.NEUTRAL,
        energy: Int = 50,
        hunger: Int = 30,
        sleepiness: Int = 30,
        social: Int = 50,
        bond: Int = 0
    ): PetState {
        return PetState(
            mood = mood,
            energy = energy,
            hunger = hunger,
            sleepiness = sleepiness,
            social = social,
            bond = bond,
            lastUpdatedAt = 1_000L
        )
    }
}
