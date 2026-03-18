package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

class LetPetRestUseCase : PetActivityUseCase {
    override fun execute(
        currentState: PetState,
        actedAtMs: Long
    ): PetActivityResult {
        val delta = PetActivityStateDelta(
            energyDelta = 18,
            sleepinessDelta = -22
        )
        val updatedState = currentState.copy(
            mood = PetMood.NEUTRAL,
            energy = currentState.energy + delta.energyDelta,
            sleepiness = currentState.sleepiness + delta.sleepinessDelta,
            lastUpdatedAt = actedAtMs
        ).withClampedValues(lastUpdatedAt = actedAtMs)
        return PetActivityResult(
            activityType = PetActivityType.REST,
            previousState = currentState,
            updatedState = updatedState,
            delta = delta,
            reason = if (currentState.sleepiness >= 55) {
                "fatigue_recovery"
            } else {
                "calm_rest"
            }
        )
    }
}
