package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Maps a tap/long-press interaction outcome to the avatar intent that best expresses
 * the immediate visible reaction.
 *
 * This is a transient override — it is applied for [REACTION_DURATION_MS] and then cleared,
 * after which normal emotion-based intent resolution resumes.
 *
 * 3 distinct reaction bands:
 * - Happy/engaged band → ENGAGED (eyes wide, positive)
 * - Low-energy/sleepy band → LOW_ENERGY (slow blink, droopy)
 * - Default/curious → ATTENTIVE (alert, looking)
 * Long press always shows ENGAGED regardless of state (warm attachment signal).
 */
object TapReactionPresentationMapper {

    const val REACTION_DURATION_MS: Long = 1_500L

    fun mapToAvatarIntent(
        resultingEmotion: PetEmotion,
        interactionType: PetInteractionType
    ): PixelPetAvatarIntent = when {
        interactionType == PetInteractionType.LONG_PRESS -> PixelPetAvatarIntent.ENGAGED
        resultingEmotion == PetEmotion.EXCITED           -> PixelPetAvatarIntent.EXCITED
        resultingEmotion == PetEmotion.HAPPY             -> PixelPetAvatarIntent.ENGAGED
        resultingEmotion == PetEmotion.SLEEPY ||
            resultingEmotion == PetEmotion.IDLE          -> PixelPetAvatarIntent.LOW_ENERGY
        resultingEmotion == PetEmotion.SAD               -> PixelPetAvatarIntent.SAD
        resultingEmotion == PetEmotion.HUNGRY            -> PixelPetAvatarIntent.HUNGRY
        else                                             -> PixelPetAvatarIntent.ATTENTIVE
    }
}
