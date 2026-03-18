package com.aipet.brain.brain.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetConditionResolverTest {
    private val resolver = PetConditionResolver()

    @Test
    fun `resolve returns calm for balanced state`() {
        val conditions = resolver.resolve(state())

        assertEquals(setOf(PetCondition.CALM), conditions)
    }

    @Test
    fun `resolve returns hungry lonely and playful when thresholds are met`() {
        val conditions = resolver.resolve(
            state(
                energy = 82,
                hunger = 78,
                sleepiness = 28,
                social = 22
            )
        )

        assertEquals(
            setOf(PetCondition.HUNGRY, PetCondition.LONELY, PetCondition.PLAYFUL),
            conditions
        )
    }

    private fun state(
        energy: Int = 50,
        hunger: Int = 30,
        sleepiness: Int = 35,
        social: Int = 50
    ): PetState {
        return PetState(
            mood = PetMood.NEUTRAL,
            energy = energy,
            hunger = hunger,
            sleepiness = sleepiness,
            social = social,
            bond = 10,
            lastUpdatedAt = 1_000L
        )
    }
}
