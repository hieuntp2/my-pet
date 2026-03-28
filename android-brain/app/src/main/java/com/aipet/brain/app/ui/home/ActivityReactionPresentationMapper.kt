package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Maps a Feed/Play/Rest activity outcome to the avatar intent that best expresses
 * the immediate visible reaction.
 *
 * Activity type sets the base reaction; resulting emotion can dampen it
 * (e.g. SAD emotion caps rest reaction to LOW_ENERGY rather than ENGAGED).
 *
 * Reaction window duration is shared with tap reactions: [TapReactionPresentationMapper.REACTION_DURATION_MS].
 */
object ActivityReactionPresentationMapper {

    fun mapToAvatarIntent(
        activityType: PetActivityType,
        resultingEmotion: PetEmotion
    ): PixelPetAvatarIntent {
        // Dampened if the pet is in a very low state regardless of activity
        if (resultingEmotion == PetEmotion.SAD) return PixelPetAvatarIntent.SAD
        if (resultingEmotion == PetEmotion.HUNGRY) return PixelPetAvatarIntent.HUNGRY

        return when (activityType) {
            PetActivityType.FEED -> PixelPetAvatarIntent.ENGAGED   // satisfied after eating
            PetActivityType.PLAY -> if (resultingEmotion == PetEmotion.EXCITED) {
                PixelPetAvatarIntent.EXCITED                        // energetic play response
            } else {
                PixelPetAvatarIntent.ENGAGED                        // positive but calmer
            }
            PetActivityType.REST -> PixelPetAvatarIntent.LOW_ENERGY // settling down
        }
    }
}
