package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

class FeedPetUseCase : PetActivityUseCase {
    override fun execute(
        currentState: PetState,
        actedAtMs: Long
    ): PetActivityResult {
        val delta = PetActivityStateDelta(
            hungerDelta = -25,
            bondDelta = 1
        )
        val updatedState = currentState.copy(
            mood = PetMood.HAPPY,
            hunger = currentState.hunger + delta.hungerDelta,
            bond = currentState.bond + delta.bondDelta,
            lastUpdatedAt = actedAtMs
        ).withClampedValues(lastUpdatedAt = actedAtMs)
        return PetActivityResult(
            activityType = PetActivityType.FEED,
            previousState = currentState,
            updatedState = updatedState,
            delta = delta,
            reason = if (currentState.hunger >= 55) {
                "hungry_relief"
            } else {
                "care_routine"
            }
        )
    }
}
