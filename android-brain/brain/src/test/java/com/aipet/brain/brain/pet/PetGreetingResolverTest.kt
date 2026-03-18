package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait
import org.junit.Assert.assertEquals
import org.junit.Test

class PetGreetingResolverTest {
    private val resolver = PetGreetingResolver()

    @Test
    fun `resolve returns hungry greeting when hunger condition is present`() {
        val greeting = resolver.resolve(
            state = state(),
            emotion = PetEmotion.HAPPY,
            conditions = setOf(PetCondition.HUNGRY)
        )

        assertEquals("hungry_greeting", greeting.reason)
        assertEquals(PetEmotion.HUNGRY, greeting.emotion)
    }

    @Test
    fun `resolve returns lonely greeting when lonely condition is present`() {
        val greeting = resolver.resolve(
            state = state(),
            emotion = PetEmotion.SAD,
            conditions = setOf(PetCondition.LONELY)
        )

        assertEquals("lonely_greeting", greeting.reason)
        assertEquals(PetEmotion.SAD, greeting.emotion)
    }

    @Test
    fun `resolve keeps hungry guardrail even when social trait is high`() {
        val greeting = resolver.resolve(
            state = state(),
            emotion = PetEmotion.HAPPY,
            conditions = setOf(PetCondition.HUNGRY),
            traits = PetTrait(
                petId = "pet-1",
                playful = 0.70f,
                lazy = 0.10f,
                curious = 0.60f,
                social = 0.95f,
                updatedAt = 1_500L
            )
        )

        assertEquals(PetEmotion.HUNGRY, greeting.emotion)
    }

    @Test
    fun `resolve returns calm greeting when calm condition is present`() {
        val greeting = resolver.resolve(
            state = state(),
            emotion = PetEmotion.IDLE,
            conditions = setOf(PetCondition.CALM)
        )

        assertEquals("glad you're here", greeting.message)
        assertEquals(PetEmotion.IDLE, greeting.emotion)
    }

    @Test
    fun `resolve can bias positive greeting toward curiosity`() {
        val greeting = resolver.resolve(
            state = state(bond = 30),
            emotion = PetEmotion.IDLE,
            conditions = emptySet(),
            traits = PetTrait(
                petId = "pet-1",
                playful = 0.10f,
                lazy = 0.10f,
                curious = 0.95f,
                social = 0.20f,
                updatedAt = 1_500L
            )
        )

        assertEquals(PetEmotion.CURIOUS, greeting.emotion)
        assertEquals("curious_default_greeting", greeting.reason)
    }

    private fun state(
        bond: Int = 45
    ): PetState {
        return PetState(
            mood = PetMood.HAPPY,
            energy = 60,
            hunger = 20,
            sleepiness = 25,
            social = 50,
            bond = bond,
            lastUpdatedAt = 1_000L
        )
    }
}
