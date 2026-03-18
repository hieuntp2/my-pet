package com.aipet.brain.brain.interaction

import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState

class PetInteractionStateReducer {
    fun apply(
        currentState: PetState,
        interactionType: PetInteractionType,
        interactedAtMs: Long
    ): PetState {
        val updatedState = when (interactionType) {
            PetInteractionType.TAP -> applyTap(currentState)
            PetInteractionType.LONG_PRESS -> applyLongPress(currentState)
        }
        return updatedState.withClampedValues(lastUpdatedAt = interactedAtMs)
    }

    private fun applyTap(currentState: PetState): PetState {
        val resolvedMood = if (currentState.sleepiness >= SLEEPY_THRESHOLD) {
            PetMood.CURIOUS
        } else {
            PetMood.HAPPY
        }
        return currentState.copy(
            mood = resolvedMood,
            social = currentState.social + TAP_SOCIAL_DELTA,
            bond = currentState.bond + TAP_BOND_DELTA
        )
    }

    private fun applyLongPress(currentState: PetState): PetState {
        val isSleepy = currentState.sleepiness >= SLEEPY_THRESHOLD
        val resolvedMood = when {
            isSleepy -> PetMood.CURIOUS
            currentState.energy >= ENERGETIC_THRESHOLD -> PetMood.EXCITED
            else -> PetMood.HAPPY
        }
        return currentState.copy(
            mood = resolvedMood,
            social = currentState.social + if (isSleepy) SLEEPY_LONG_PRESS_SOCIAL_DELTA else LONG_PRESS_SOCIAL_DELTA,
            bond = currentState.bond + if (isSleepy) SLEEPY_LONG_PRESS_BOND_DELTA else LONG_PRESS_BOND_DELTA
        )
    }

    companion object {
        private const val SLEEPY_THRESHOLD = 70
        private const val ENERGETIC_THRESHOLD = 65
        private const val TAP_SOCIAL_DELTA = 1
        private const val TAP_BOND_DELTA = 1
        private const val LONG_PRESS_SOCIAL_DELTA = 3
        private const val LONG_PRESS_BOND_DELTA = 2
        private const val SLEEPY_LONG_PRESS_SOCIAL_DELTA = 1
        private const val SLEEPY_LONG_PRESS_BOND_DELTA = 1
    }
}
