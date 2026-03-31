package com.aipet.brain.brain.pet

import kotlin.math.floor
import kotlin.math.roundToInt

class PetStateDecayEngine {
    fun applyDecay(currentState: PetState, now: Long): PetState {
        if (now <= currentState.lastUpdatedAt) {
            return currentState
        }

        val elapsedMinutes = ((now - currentState.lastUpdatedAt) / 60_000.0)
        if (elapsedMinutes <= 0.0) {
            return currentState
        }

        // Core needs decay — same as before
        val energyDrop = floor(elapsedMinutes / 30.0).toInt()
        val hungerRise = floor(elapsedMinutes / 20.0).toInt()
        val sleepinessRise = floor(elapsedMinutes / 25.0).toInt()
        val socialDrop = floor(elapsedMinutes / 40.0).toInt()

        // Comfort decays slowly — takes ~5 hours to drop 1 unit per tick
        val comfortDrop = floor(elapsedMinutes / COMFORT_DECAY_INTERVAL_MIN).toInt()

        // Stimulation: a bored pet's stimulation falls moderately fast (~45 min per unit)
        val stimulationDrop = floor(elapsedMinutes / STIMULATION_DECAY_INTERVAL_MIN).toInt()

        // Mood valence drifts toward 0 gradually — emotional weather settles
        val valenceDrift = if (currentState.moodValence > 0) {
            -floor(elapsedMinutes / VALENCE_DRIFT_INTERVAL_MIN).toInt()
        } else if (currentState.moodValence < 0) {
            floor(elapsedMinutes / VALENCE_DRIFT_INTERVAL_MIN).toInt()
        } else {
            0
        }

        // Trust decays slightly over extended absence — less secure without interaction
        val trustDrop = floor(elapsedMinutes / TRUST_DECAY_INTERVAL_MIN).toInt()

        // Attachment score builds a tiny bit when bond is set and time passes (longing)
        val attachmentGain = if (currentState.bond >= ATTACHMENT_BUILD_BOND_THRESHOLD) {
            floor(elapsedMinutes / ATTACHMENT_GAIN_INTERVAL_MIN).toInt()
        } else {
            0
        }

        return currentState.copy(
            energy = (currentState.energy - energyDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            hunger = (currentState.hunger + hungerRise).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            sleepiness = (currentState.sleepiness + sleepinessRise).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            social = (currentState.social - socialDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            comfort = (currentState.comfort - comfortDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            stimulation = (currentState.stimulation - stimulationDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            moodValence = (currentState.moodValence + valenceDrift).coerceIn(PetState.VALENCE_MIN, PetState.VALENCE_MAX),
            trustScore = (currentState.trustScore - trustDrop).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            attachmentScore = (currentState.attachmentScore + attachmentGain).coerceIn(PetState.VALUE_MIN, PetState.VALUE_MAX),
            lastUpdatedAt = now
        )
    }

    private companion object {
        const val COMFORT_DECAY_INTERVAL_MIN = 120.0   // 1 unit per 2 hours
        const val STIMULATION_DECAY_INTERVAL_MIN = 45.0 // 1 unit per 45 minutes
        const val VALENCE_DRIFT_INTERVAL_MIN = 90.0     // 1 unit drift per 90 minutes
        const val TRUST_DECAY_INTERVAL_MIN = 180.0      // 1 unit per 3 hours
        const val ATTACHMENT_GAIN_INTERVAL_MIN = 60.0   // 1 unit per hour (if bond >= threshold)
        const val ATTACHMENT_BUILD_BOND_THRESHOLD = 20
    }
}
