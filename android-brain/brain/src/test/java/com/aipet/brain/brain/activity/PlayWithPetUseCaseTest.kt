package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class PlayWithPetUseCaseTest {
    private val useCase = PlayWithPetUseCase()

    @Test
    fun `play increases social and bond while costing energy`() {
        val result = useCase.execute(
            currentState = state(energy = 60, social = 40, bond = 5),
            actedAtMs = 2_500L
        )

        assertEquals(PetActivityType.PLAY, result.activityType)
        assertEquals(PetMood.EXCITED, result.updatedState.mood)
        assertEquals(52, result.updatedState.energy)
        assertEquals(45, result.updatedState.social)
        assertEquals(7, result.updatedState.bond)
        assertEquals(-8, result.delta.energyDelta)
        assertEquals(5, result.delta.socialDelta)
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
