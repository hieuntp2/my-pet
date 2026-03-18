package com.aipet.brain.app.gameplay

import com.aipet.brain.brain.activity.PetActivityResult
import com.aipet.brain.brain.activity.PetActivityStateDelta
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PetGameplaySupportTest {
    @Test
    fun `cooldown gate blocks repeated tap inside cooldown`() {
        val gate = PetGameplayCooldownGate()

        gate.recordSuccess(PetGameplayAction.TAP, appliedAtMs = 1_000L)
        val decision = gate.evaluate(PetGameplayAction.TAP, attemptedAtMs = 1_500L)

        assertFalse(decision.isAllowed)
        assertTrue(decision.remainingMs > 0L)
    }

    @Test
    fun `interaction feedback stays short and action-specific`() {
        val feedback = PetGameplayFeedbackFormatter.forInteraction(
            petName = "Cún",
            interactionType = PetInteractionType.LONG_PRESS,
            previousState = state(bond = 10),
            updatedState = state(mood = PetMood.EXCITED, bond = 12)
        )

        assertTrue(feedback.text.contains("cuddle", ignoreCase = true) || feedback.text.contains("close", ignoreCase = true))
        assertFalse(feedback.isBlocked)
    }

    @Test
    fun `activity feedback reflects rest recovery`() {
        val feedback = PetGameplayFeedbackFormatter.forActivity(
            petName = "Cún",
            result = PetActivityResult(
                activityType = PetActivityType.REST,
                previousState = state(energy = 20, sleepiness = 80),
                updatedState = state(mood = PetMood.NEUTRAL, energy = 38, sleepiness = 58),
                delta = PetActivityStateDelta(energyDelta = 18, sleepinessDelta = -22),
                reason = "fatigue_recovery"
            )
        )

        assertTrue(feedback.text.contains("rested", ignoreCase = true) || feedback.text.contains("energetic", ignoreCase = true))
        assertFalse(feedback.isBlocked)
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
