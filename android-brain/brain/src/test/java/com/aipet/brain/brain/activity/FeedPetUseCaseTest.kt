package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class FeedPetUseCaseTest {
    private val useCase = FeedPetUseCase()

    @Test
    fun `feed reduces hunger and increases bond`() {
        val result = useCase.execute(
            currentState = state(hunger = 70, bond = 3),
            actedAtMs = 2_000L
        )

        assertEquals(PetActivityType.FEED, result.activityType)
        assertEquals(PetMood.HAPPY, result.updatedState.mood)
        assertEquals(45, result.updatedState.hunger)
        assertEquals(4, result.updatedState.bond)
        assertEquals(-25, result.delta.hungerDelta)
        assertEquals(1, result.delta.bondDelta)
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
