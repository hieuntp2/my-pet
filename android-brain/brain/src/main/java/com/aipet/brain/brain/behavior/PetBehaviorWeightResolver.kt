package com.aipet.brain.brain.behavior

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import kotlin.math.abs

open class PetBehaviorWeightResolver {
    fun resolveGreetingEmotion(
        context: PetBehaviorContext,
        fallbackEmotion: PetEmotion
    ): PetBehaviorDecision<PetEmotion> {
        val candidates = when {
            context.conditions.contains(PetCondition.HUNGRY) -> listOf(
                candidate(PetEmotion.HUNGRY, "hungry_greeting", 1.30f),
                candidate(PetEmotion.CURIOUS, "curious_hungry_greeting", 0.55f),
                candidate(PetEmotion.HAPPY, "soft_hungry_greeting", 0.35f)
            )

            context.conditions.contains(PetCondition.SLEEPY) -> listOf(
                candidate(PetEmotion.SLEEPY, "sleepy_greeting", 1.35f),
                candidate(PetEmotion.IDLE, "calm_sleepy_greeting", 0.75f),
                candidate(PetEmotion.CURIOUS, "waking_curious_greeting", 0.45f)
            )

            context.conditions.contains(PetCondition.LONELY) -> listOf(
                candidate(PetEmotion.SAD, "lonely_greeting", 1.20f),
                candidate(PetEmotion.HAPPY, "warm_lonely_greeting", 0.70f),
                candidate(PetEmotion.CURIOUS, "seeking_lonely_greeting", 0.60f)
            )

            fallbackEmotion == PetEmotion.EXCITED || fallbackEmotion == PetEmotion.HAPPY || context.state.bond >= 60 -> listOf(
                candidate(PetEmotion.HAPPY, "warm_greeting", 1.00f),
                candidate(PetEmotion.EXCITED, "playful_greeting", 0.90f),
                candidate(PetEmotion.CURIOUS, "curious_greeting", 0.70f),
                candidate(PetEmotion.IDLE, "steady_greeting", 0.45f)
            )

            else -> listOf(
                candidate(fallbackEmotion, "default_greeting", 0.95f),
                candidate(PetEmotion.CURIOUS, "curious_default_greeting", 0.85f),
                candidate(PetEmotion.HAPPY, "warm_default_greeting", 0.75f),
                candidate(PetEmotion.IDLE, "calm_default_greeting", 0.70f)
            )
        }
        return chooseHighestWeight(applySharedTraitBias(candidates, context))
    }

    fun resolveInteractionEmotion(
        interactionType: PetInteractionType,
        context: PetBehaviorContext
    ): PetBehaviorDecision<PetEmotion> {
        val baseCandidates = when (interactionType) {
            PetInteractionType.TAP -> listOf(
                candidate(PetEmotion.HAPPY, "tap_happy", 1.00f),
                candidate(PetEmotion.CURIOUS, "tap_curious", 0.82f),
                candidate(PetEmotion.EXCITED, "tap_excited", 0.72f),
                candidate(PetEmotion.SLEEPY, "tap_sleepy", 0.38f)
            )

            PetInteractionType.LONG_PRESS -> listOf(
                candidate(PetEmotion.CURIOUS, "long_press_curious", 0.98f),
                candidate(PetEmotion.HAPPY, "long_press_happy", 0.85f),
                candidate(PetEmotion.EXCITED, "long_press_excited", 0.78f),
                candidate(PetEmotion.SLEEPY, "long_press_sleepy", 0.58f)
            )
        }
        val candidates = applySharedTraitBias(baseCandidates, context).map { candidate ->
            when (candidate.behavior) {
                PetEmotion.SLEEPY -> candidate.withAdjustment(
                    source = "state.sleepiness_guardrail",
                    delta = if (context.conditions.contains(PetCondition.SLEEPY)) 0.28f else 0f
                )
                PetEmotion.EXCITED -> candidate.withAdjustment(
                    source = "state.sleepiness_guardrail",
                    delta = if (context.conditions.contains(PetCondition.SLEEPY)) -0.24f else 0f
                )
                PetEmotion.HAPPY -> candidate.withAdjustment(
                    source = "state.lonely_touch_warmth",
                    delta = if (context.conditions.contains(PetCondition.LONELY)) 0.14f else 0f
                )
                else -> candidate
            }
        }
        return chooseHighestWeight(candidates)
    }

    fun resolveActivityEmotion(
        activityType: PetActivityType,
        context: PetBehaviorContext
    ): PetBehaviorDecision<PetEmotion> {
        val baseCandidates = when (activityType) {
            PetActivityType.FEED -> listOf(
                candidate(PetEmotion.HAPPY, "feed_happy", 1.00f),
                candidate(PetEmotion.CURIOUS, "feed_curious", 0.66f),
                candidate(PetEmotion.EXCITED, "feed_excited", 0.46f)
            )

            PetActivityType.PLAY -> listOf(
                candidate(PetEmotion.EXCITED, "play_excited", 1.00f),
                candidate(PetEmotion.HAPPY, "play_happy", 0.92f),
                candidate(PetEmotion.CURIOUS, "play_curious", 0.78f),
                candidate(PetEmotion.SLEEPY, "play_tired", 0.28f)
            )

            PetActivityType.REST -> listOf(
                candidate(PetEmotion.SLEEPY, "rest_sleepy", 1.00f),
                candidate(PetEmotion.IDLE, "rest_idle", 0.82f),
                candidate(PetEmotion.HAPPY, "rest_comforted", 0.54f)
            )
        }
        val candidates = applySharedTraitBias(baseCandidates, context).map { candidate ->
            when (candidate.behavior) {
                PetEmotion.SLEEPY -> candidate.withAdjustment(
                    source = "activity.sleepiness_alignment",
                    delta = if (activityType == PetActivityType.REST || context.conditions.contains(PetCondition.SLEEPY)) 0.18f else 0f
                )
                PetEmotion.EXCITED -> candidate.withAdjustment(
                    source = "activity.energy_guardrail",
                    delta = if (activityType == PetActivityType.PLAY && context.state.energy >= 45) 0.12f else -0.06f
                )
                else -> candidate
            }
        }
        return chooseHighestWeight(candidates)
    }

    protected fun <T> chooseHighestWeight(
        candidates: List<PetBehaviorCandidate<T>>
    ): PetBehaviorDecision<T> {
        require(candidates.isNotEmpty()) { "candidates cannot be empty." }

        val selected = candidates.maxWithOrNull(
            compareBy<PetBehaviorCandidate<T>> { it.totalWeight }
                .thenByDescending { it.baseWeight }
        ) ?: candidates.first()

        return PetBehaviorDecision(
            selectedBehavior = selected.behavior,
            selectedLabel = selected.label,
            candidates = candidates
        )
    }

    private fun applySharedTraitBias(
        candidates: List<PetBehaviorCandidate<PetEmotion>>,
        context: PetBehaviorContext
    ): List<PetBehaviorCandidate<PetEmotion>> {
        return candidates.map { candidate ->
            var updated = candidate
            val traits = context.traits
            if (traits != null) {
                when (candidate.behavior) {
                    PetEmotion.EXCITED -> {
                        updated = updated.withAdjustment("trait.playful_bias", centeredTraitBias(traits.playful, 0.22f))
                        updated = updated.withAdjustment("trait.social_bias", centeredTraitBias(traits.social, 0.10f))
                        updated = updated.withAdjustment("trait.lazy_bias", -centeredTraitBias(traits.lazy, 0.12f))
                    }
                    PetEmotion.HAPPY -> {
                        updated = updated.withAdjustment("trait.social_bias", centeredTraitBias(traits.social, 0.20f))
                        updated = updated.withAdjustment("trait.playful_bias", centeredTraitBias(traits.playful, 0.12f))
                    }
                    PetEmotion.CURIOUS -> {
                        updated = updated.withAdjustment("trait.curious_bias", centeredTraitBias(traits.curious, 0.24f))
                        updated = updated.withAdjustment("trait.playful_bias", centeredTraitBias(traits.playful, 0.06f))
                    }
                    PetEmotion.SLEEPY,
                    PetEmotion.IDLE -> {
                        updated = updated.withAdjustment("trait.lazy_bias", centeredTraitBias(traits.lazy, 0.24f))
                        updated = updated.withAdjustment("trait.playful_bias", -centeredTraitBias(traits.playful, 0.08f))
                    }
                    PetEmotion.SAD -> {
                        updated = updated.withAdjustment("trait.social_bias", -centeredTraitBias(traits.social, 0.10f))
                    }
                    PetEmotion.HUNGRY -> Unit
                }
            }
            if (context.conditions.contains(PetCondition.PLAYFUL) && candidate.behavior == PetEmotion.EXCITED) {
                updated = updated.withAdjustment("condition.playful", 0.18f)
            }
            updated
        }
    }

    private fun candidate(
        emotion: PetEmotion,
        label: String,
        baseWeight: Float
    ): PetBehaviorCandidate<PetEmotion> {
        return PetBehaviorCandidate(
            behavior = emotion,
            label = label,
            baseWeight = baseWeight
        )
    }

    private fun centeredTraitBias(
        traitValue: Float,
        magnitude: Float
    ): Float {
        val normalized = ((traitValue - 0.5f) * 2f).coerceIn(-1f, 1f)
        return normalized * abs(magnitude)
    }

    private fun PetBehaviorCandidate<PetEmotion>.withAdjustment(
        source: String,
        delta: Float
    ): PetBehaviorCandidate<PetEmotion> {
        if (delta == 0f) {
            return this
        }
        return copy(adjustments = adjustments + BehaviorWeight(source = source, delta = delta))
    }
}
