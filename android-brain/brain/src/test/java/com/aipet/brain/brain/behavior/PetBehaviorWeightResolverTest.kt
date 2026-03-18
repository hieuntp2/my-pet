package com.aipet.brain.brain.behavior

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PetBehaviorWeightResolverTest {
    private val resolver = PetBehaviorWeightResolver()

    @Test
    fun `playful trait biases play reaction toward excited`() {
        val decision = resolver.resolveActivityEmotion(
            activityType = PetActivityType.PLAY,
            context = context(
                traits = traits(playful = 0.95f, lazy = 0.10f, curious = 0.45f, social = 0.60f)
            )
        )

        assertEquals(PetEmotion.EXCITED, decision.selectedBehavior)
        assertTrue(
            decision.candidates.first { it.behavior == PetEmotion.EXCITED }.totalWeight >
                decision.candidates.first { it.behavior == PetEmotion.HAPPY }.totalWeight
        )
    }

    @Test
    fun `lazy trait biases rest reaction toward sleepy`() {
        val decision = resolver.resolveActivityEmotion(
            activityType = PetActivityType.REST,
            context = context(
                traits = traits(playful = 0.10f, lazy = 0.95f, curious = 0.35f, social = 0.40f),
                conditions = setOf(PetCondition.SLEEPY)
            )
        )

        assertEquals(PetEmotion.SLEEPY, decision.selectedBehavior)
    }

    @Test
    fun `social trait warms lonely greeting without removing guardrail`() {
        val decision = resolver.resolveGreetingEmotion(
            context = context(
                traits = traits(playful = 0.40f, lazy = 0.30f, curious = 0.50f, social = 0.95f),
                conditions = setOf(PetCondition.LONELY)
            ),
            fallbackEmotion = PetEmotion.SAD
        )

        assertEquals(PetEmotion.SAD, decision.selectedBehavior)
        assertTrue(decision.candidates.any { it.label == "warm_lonely_greeting" })
    }

    @Test
    fun `curious trait biases long press toward curious instead of happy`() {
        val decision = resolver.resolveInteractionEmotion(
            interactionType = PetInteractionType.LONG_PRESS,
            context = context(
                traits = traits(playful = 0.25f, lazy = 0.20f, curious = 0.95f, social = 0.40f)
            )
        )

        assertEquals(PetEmotion.CURIOUS, decision.selectedBehavior)
    }

    private fun context(
        state: PetState = PetState(
            mood = PetMood.HAPPY,
            energy = 72,
            hunger = 20,
            sleepiness = 25,
            social = 55,
            bond = 45,
            lastUpdatedAt = 1_000L
        ),
        conditions: Set<PetCondition> = emptySet(),
        traits: PetTrait? = null
    ): PetBehaviorContext {
        return PetBehaviorContext(
            state = state,
            conditions = conditions,
            traits = traits
        )
    }

    private fun traits(
        playful: Float,
        lazy: Float,
        curious: Float,
        social: Float
    ): PetTrait {
        return PetTrait(
            petId = "pet-1",
            playful = playful,
            lazy = lazy,
            curious = curious,
            social = social,
            updatedAt = 2_000L
        )
    }
}
