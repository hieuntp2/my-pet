package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class LetPetRestUseCaseTest {
    private val useCase = LetPetRestUseCase()

    @Test
    fun `rest recovers energy and lowers sleepiness`() {
        val result = useCase.execute(
            currentState = state(energy = 30, sleepiness = 80),
            actedAtMs = 3_000L
        )

        assertEquals(PetActivityType.REST, result.activityType)
        assertEquals(PetMood.NEUTRAL, result.updatedState.mood)
        assertEquals(48, result.updatedState.energy)
        assertEquals(58, result.updatedState.sleepiness)
        assertEquals(18, result.delta.energyDelta)
        assertEquals(-22, result.delta.sleepinessDelta)
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
