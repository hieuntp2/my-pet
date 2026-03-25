package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.personality.PetPersonalitySummaryResolver
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.brain.pet.PetState

class PetAnimationInputMapper(
    private val personalitySummaryResolver: PetPersonalitySummaryResolver = PetPersonalitySummaryResolver()
) {
    fun mapFrame(
        state: PetState,
        emotion: PetEmotion,
        conditions: Set<PetCondition>,
        traits: PetTrait?,
        trigger: PetAnimationTrigger = PetAnimationTrigger.none()
    ): PetAnimationFrame {
        return PetAnimationFrame(
            emotion = emotion.toAnimationEmotion(),
            energyBand = state.energy.toAnimationBand(lowMax = 34, midMax = 69),
            hungerBand = state.hunger.toAnimationBand(lowMax = 34, midMax = 69),
            socialBand = state.social.toAnimationBand(lowMax = 34, midMax = 69),
            flavor = traits.toAnimationFlavor(),
            trigger = if (trigger.reactionType == PetAnimationReactionType.NONE) {
                conditions.toPassiveTrigger(emotion)
            } else {
                trigger
            }
        )
    }

    fun mapGreetingTrigger(
        greeting: PetGreetingReaction,
        conditions: Set<PetCondition>,
        decision: PetBehaviorDecision<PetEmotion>
    ): PetAnimationTrigger {
        val greetingType = when {
            conditions.contains(PetCondition.HUNGRY) || greeting.emotion == PetEmotion.HUNGRY -> PetAnimationGreetingType.HUNGRY
            conditions.contains(PetCondition.SLEEPY) || greeting.emotion == PetEmotion.SLEEPY -> PetAnimationGreetingType.SLEEPY
            conditions.contains(PetCondition.LONELY) || greeting.emotion == PetEmotion.SAD -> PetAnimationGreetingType.LONELY
            decision.selectedBehavior == PetEmotion.EXCITED -> PetAnimationGreetingType.PLAYFUL
            decision.selectedBehavior == PetEmotion.CURIOUS -> PetAnimationGreetingType.CURIOUS
            decision.selectedBehavior == PetEmotion.HAPPY -> PetAnimationGreetingType.WARM
            else -> PetAnimationGreetingType.CALM
        }
        return PetAnimationTrigger(
            reactionType = PetAnimationReactionType.GREETING,
            emotion = greeting.emotion.toAnimationEmotion(),
            greetingType = greetingType
        )
    }

    fun mapInteractionTrigger(
        interactionType: PetInteractionType,
        decision: PetBehaviorDecision<PetEmotion>
    ): PetAnimationTrigger {
        return PetAnimationTrigger(
            reactionType = when (interactionType) {
                PetInteractionType.TAP -> PetAnimationReactionType.TAP
                PetInteractionType.LONG_PRESS -> PetAnimationReactionType.LONG_PRESS
            },
            emotion = decision.selectedBehavior.toAnimationEmotion()
        )
    }

    fun mapActivityTrigger(
        activityType: PetActivityType,
        decision: PetBehaviorDecision<PetEmotion>
    ): PetAnimationTrigger {
        return PetAnimationTrigger(
            reactionType = when (activityType) {
                PetActivityType.FEED -> PetAnimationReactionType.FEED
                PetActivityType.PLAY -> PetAnimationReactionType.PLAY
                PetActivityType.REST -> PetAnimationReactionType.REST
            },
            emotion = decision.selectedBehavior.toAnimationEmotion(),
            activityResult = when (activityType) {
                PetActivityType.FEED -> PetAnimationActivityResult.FEED
                PetActivityType.PLAY -> PetAnimationActivityResult.PLAY
                PetActivityType.REST -> PetAnimationActivityResult.REST
            }
        )
    }

    fun mapSoundTrigger(category: AudioCategory): PetAnimationTrigger {
        return PetAnimationTrigger(
            reactionType = PetAnimationReactionType.SOUND,
            emotion = category.toAnimationEmotion()
        )
    }

    fun mapGenericReaction(
        emotion: PetEmotion,
        source: PetAnimationSource
    ): PetAnimationTrigger {
        return PetAnimationTrigger(
            reactionType = when (source) {
                PetAnimationSource.GREETING -> PetAnimationReactionType.GREETING
                PetAnimationSource.TAP -> PetAnimationReactionType.TAP
                PetAnimationSource.LONG_PRESS -> PetAnimationReactionType.LONG_PRESS
                PetAnimationSource.FEED -> PetAnimationReactionType.FEED
                PetAnimationSource.PLAY -> PetAnimationReactionType.PLAY
                PetAnimationSource.REST -> PetAnimationReactionType.REST
                PetAnimationSource.SOUND -> PetAnimationReactionType.SOUND
                PetAnimationSource.REACTION,
                PetAnimationSource.MOOD -> PetAnimationReactionType.REACTION
            },
            emotion = emotion.toAnimationEmotion()
        )
    }

    private fun Int.toAnimationBand(lowMax: Int, midMax: Int): PetAnimationBand {
        return when {
            this <= lowMax -> PetAnimationBand.LOW
            this <= midMax -> PetAnimationBand.MID
            else -> PetAnimationBand.HIGH
        }
    }

    private fun PetTrait?.toAnimationFlavor(): PetAnimationFlavor {
        val summary = personalitySummaryResolver.resolve(this)
        return when (summary?.dominantTrait) {
            "playful" -> PetAnimationFlavor.PLAYFUL
            "lazy" -> PetAnimationFlavor.CALM
            "curious" -> PetAnimationFlavor.CURIOUS
            "social" -> PetAnimationFlavor.AFFECTIONATE
            else -> PetAnimationFlavor.BALANCED
        }
    }

    private fun Set<PetCondition>.toPassiveTrigger(emotion: PetEmotion): PetAnimationTrigger {
        val greetingType = when {
            contains(PetCondition.HUNGRY) -> PetAnimationGreetingType.HUNGRY
            contains(PetCondition.SLEEPY) -> PetAnimationGreetingType.SLEEPY
            contains(PetCondition.LONELY) -> PetAnimationGreetingType.LONELY
            contains(PetCondition.PLAYFUL) -> PetAnimationGreetingType.PLAYFUL
            contains(PetCondition.CALM) -> PetAnimationGreetingType.CALM
            emotion == PetEmotion.CURIOUS -> PetAnimationGreetingType.CURIOUS
            emotion == PetEmotion.HAPPY || emotion == PetEmotion.EXCITED -> PetAnimationGreetingType.WARM
            else -> PetAnimationGreetingType.CALM
        }
        return PetAnimationTrigger(
            reactionType = PetAnimationReactionType.NONE,
            emotion = emotion.toAnimationEmotion(),
            greetingType = greetingType
        )
    }

    private fun PetEmotion.toAnimationEmotion(): PetAnimationEmotion {
        return PetEmotionMappings.petToAnimationEmotion(this)
    }

    private fun AudioCategory.toAnimationEmotion(): PetAnimationEmotion {
        return PetEmotionMappings.audioToAnimationEmotion(this)
    }
}
