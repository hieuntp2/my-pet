package com.aipet.brain.brain.personality

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import kotlin.math.abs

class PetTraitEvolutionEngine {
    fun applyInteraction(
        current: PetTrait,
        interactionType: PetInteractionType,
        appliedAtMs: Long
    ): PetTrait {
        val delta = when (interactionType) {
            PetInteractionType.TAP -> TraitDelta(
                playful = 0.010f,
                social = 0.010f
            )

            PetInteractionType.LONG_PRESS -> TraitDelta(
                lazy = 0.006f,
                social = 0.016f
            )
        }
        return evolve(current = current, delta = delta, appliedAtMs = appliedAtMs)
    }

    fun applyActivity(
        current: PetTrait,
        activityType: PetActivityType,
        appliedAtMs: Long
    ): PetTrait {
        val delta = when (activityType) {
            PetActivityType.FEED -> TraitDelta(
                lazy = 0.004f,
                social = 0.008f
            )

            PetActivityType.PLAY -> TraitDelta(
                playful = 0.018f,
                lazy = -0.006f,
                curious = 0.010f
            )

            PetActivityType.REST -> TraitDelta(
                playful = -0.004f,
                lazy = 0.018f
            )
        }
        return evolve(current = current, delta = delta, appliedAtMs = appliedAtMs)
    }

    private fun evolve(
        current: PetTrait,
        delta: TraitDelta,
        appliedAtMs: Long
    ): PetTrait {
        return current.copy(
            playful = current.playful.applyDirectedStep(delta.playful),
            lazy = current.lazy.applyDirectedStep(delta.lazy),
            curious = current.curious.applyDirectedStep(delta.curious),
            social = current.social.applyDirectedStep(delta.social),
            updatedAt = appliedAtMs
        )
    }

    private fun Float.applyDirectedStep(step: Float): Float {
        if (step == 0f) {
            return this
        }
        val boundedStep = step.coerceIn(-MAX_STEP, MAX_STEP)
        val distance = if (boundedStep > 0f) {
            1f - this
        } else {
            this
        }
        val scaledDelta = abs(boundedStep) * distance.coerceIn(MIN_DISTANCE_FACTOR, 1f)
        val signedDelta = if (boundedStep > 0f) scaledDelta else -scaledDelta
        return (this + signedDelta).coerceIn(PetTrait.TRAIT_MIN, PetTrait.TRAIT_MAX)
    }

    private data class TraitDelta(
        val playful: Float = 0f,
        val lazy: Float = 0f,
        val curious: Float = 0f,
        val social: Float = 0f
    )

    private companion object {
        const val MAX_STEP: Float = 0.03f
        const val MIN_DISTANCE_FACTOR: Float = 0.12f
    }
}
