package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

class PlayWithPetUseCase : PetActivityUseCase {
    override fun execute(
        currentState: PetState,
        actedAtMs: Long
    ): PetActivityResult {
        val delta = PetActivityStateDelta(
            energyDelta = -8,
            socialDelta = 5,
            bondDelta = 2
        )
        val updatedState = currentState.copy(
            mood = if (currentState.energy >= 45) PetMood.EXCITED else PetMood.HAPPY,
            energy = currentState.energy + delta.energyDelta,
            social = currentState.social + delta.socialDelta,
            bond = currentState.bond + delta.bondDelta,
            lastUpdatedAt = actedAtMs
        ).withClampedValues(lastUpdatedAt = actedAtMs)
        return PetActivityResult(
            activityType = PetActivityType.PLAY,
            previousState = currentState,
            updatedState = updatedState,
            delta = delta,
            reason = if (currentState.energy >= 45) {
                "playful_energy"
            } else {
                "gentle_play"
            }
        )
    }
}
