package com.aipet.brain.brain.pet

import org.junit.Assert.assertEquals
import org.junit.Test

class PetEmotionResolverTest {
    private val resolver = PetEmotionResolver()

    @Test
    fun `resolve returns hungry when hunger is high`() {
        val state = state(hunger = 85)

        val emotion = resolver.resolve(state, setOf(PetCondition.HUNGRY))

        assertEquals(PetEmotion.HUNGRY, emotion)
    }

    @Test
    fun `resolve returns sleepy when sleepiness dominates`() {
        val state = state(sleepiness = 88, energy = 25)

        val emotion = resolver.resolve(state, setOf(PetCondition.SLEEPY))

        assertEquals(PetEmotion.SLEEPY, emotion)
    }

    @Test
    fun `resolve returns excited for high energy and social`() {
        val state = state(energy = 90, social = 65)

        val emotion = resolver.resolve(state, emptySet())

        assertEquals(PetEmotion.EXCITED, emotion)
    }

    @Test
    fun `resolve falls back to idle`() {
        val state = state()

        val emotion = resolver.resolve(state, emptySet())

        assertEquals(PetEmotion.IDLE, emotion)
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
