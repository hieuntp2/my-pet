package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.behavior.PetBehaviorCandidate
import com.aipet.brain.brain.behavior.PetBehaviorDecision
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.pet.PetGreetingReaction
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import org.junit.Assert.assertEquals
import org.junit.Test

class PetAnimationInputMapperTest {
    private val mapper = PetAnimationInputMapper()

    @Test
    fun `mapFrame converts pet state into bounded animation grammar`() {
        val frame = mapper.mapFrame(
            state = PetState(
                mood = PetMood.HAPPY,
                energy = 88,
                hunger = 18,
                sleepiness = 22,
                social = 84,
                bond = 20,
                lastUpdatedAt = 1_000L
            ),
            emotion = PetEmotion.HAPPY,
            conditions = setOf(PetCondition.PLAYFUL),
            traits = PetTrait(
                petId = "pet-1",
                playful = 0.92f,
                lazy = 0.18f,
                curious = 0.45f,
                social = 0.55f,
                updatedAt = 2_000L
            )
        )

        assertEquals(PetAnimationEmotion.HAPPY, frame.emotion)
        assertEquals(PetAnimationBand.HIGH, frame.energyBand)
        assertEquals(PetAnimationBand.LOW, frame.hungerBand)
        assertEquals(PetAnimationBand.HIGH, frame.socialBand)
        assertEquals(PetAnimationFlavor.PLAYFUL, frame.flavor)
        assertEquals(PetAnimationGreetingType.PLAYFUL, frame.trigger.greetingType)
    }

    @Test
    fun `mapGreetingTrigger reflects needy and playful greeting categories`() {
        val hungry = mapper.mapGreetingTrigger(
            greeting = PetGreetingReaction(
                message = "*nuzzle* wants food",
                emotion = PetEmotion.HUNGRY,
                reason = "hungry_greeting"
            ),
            conditions = setOf(PetCondition.HUNGRY),
            decision = decision(PetEmotion.CURIOUS)
        )
        val playful = mapper.mapGreetingTrigger(
            greeting = PetGreetingReaction(
                message = "so happy to see you!",
                emotion = PetEmotion.EXCITED,
                reason = "playful_greeting"
            ),
            conditions = emptySet(),
            decision = decision(PetEmotion.EXCITED)
        )

        assertEquals(PetAnimationGreetingType.HUNGRY, hungry.greetingType)
        assertEquals(PetAnimationGreetingType.PLAYFUL, playful.greetingType)
    }

    @Test
    fun `mapInteractionActivityAndSoundTriggers stay in shared grammar`() {
        val tap = mapper.mapInteractionTrigger(PetInteractionType.TAP, decision(PetEmotion.HAPPY))
        val rest = mapper.mapActivityTrigger(PetActivityType.REST, decision(PetEmotion.SLEEPY))
        val sound = mapper.mapSoundTrigger(AudioCategory.SURPRISED)

        assertEquals(PetAnimationReactionType.TAP, tap.reactionType)
        assertEquals(PetAnimationReactionType.REST, rest.reactionType)
        assertEquals(PetAnimationActivityResult.REST, rest.activityResult)
        assertEquals(PetAnimationEmotion.EXCITED, sound.emotion)
    }

    private fun decision(emotion: PetEmotion): PetBehaviorDecision<PetEmotion> {
        return PetBehaviorDecision(
            selectedBehavior = emotion,
            selectedLabel = emotion.name.lowercase(),
            candidates = listOf(
                PetBehaviorCandidate(
                    behavior = emotion,
                    label = emotion.name.lowercase(),
                    baseWeight = 1f
                )
            )
        )
    }
}
