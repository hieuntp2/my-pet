package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Maps the emotion from a [com.aipet.brain.brain.pet.PetGreetingReaction] to the
 * avatar intent that best expresses the greeting moment on screen.
 *
 * This mapping is applied at highest priority during the greeting window so the avatar
 * reacts visibly to the app-open greeting before reverting to normal emotion-driven display.
 */
object GreetingPresentationMapper {

    fun mapToAvatarIntent(greetingEmotion: PetEmotion): PixelPetAvatarIntent = when (greetingEmotion) {
        PetEmotion.EXCITED -> PixelPetAvatarIntent.EXCITED
        PetEmotion.HAPPY   -> PixelPetAvatarIntent.ENGAGED
        PetEmotion.SAD     -> PixelPetAvatarIntent.SAD
        PetEmotion.SLEEPY  -> PixelPetAvatarIntent.LOW_ENERGY
        PetEmotion.HUNGRY  -> PixelPetAvatarIntent.HUNGRY
        PetEmotion.CURIOUS -> PixelPetAvatarIntent.ATTENTIVE
        else               -> PixelPetAvatarIntent.ENGAGED
    }
}
