package com.aipet.brain.brain.pet

import kotlin.math.floor

class PetStateDecayEngine {
    fun applyDecay(currentState: PetState, now: Long): PetState {
        if (now <= currentState.lastUpdatedAt) {
            return currentState
        }

        val elapsedMinutes = ((now - currentState.lastUpdatedAt) / 60_000.0)
        if (elapsedMinutes <= 0.0) {
            return currentState
        }

        val energyDrop = floor(elapsedMinutes / 30.0).toInt()
        val hungerRise = floor(elapsedMinutes / 20.0).toInt()
        val sleepinessRise = floor(elapsedMinutes / 25.0).toInt()
        val socialDrop = floor(elapsedMinutes / 40.0).toInt()

        return currentState.copy(
            energy = (currentState.energy - energyDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            hunger = (currentState.hunger + hungerRise).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            sleepiness = (currentState.sleepiness + sleepinessRise).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            social = (currentState.social - socialDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            lastUpdatedAt = now
        )
    }
}
