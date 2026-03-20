package com.aipet.brain.app.animation

import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Maps [PetEmotion] (brain layer) to [PetVisualState] (pixel animation layer).
 *
 * This adapter keeps the pixel-avatar module free from brain-layer types.
 */
object PetEmotionToVisualStateMapper {
    fun map(emotion: PetEmotion): PetVisualState = when (emotion) {
        PetEmotion.IDLE -> PetVisualState.NEUTRAL
        PetEmotion.HAPPY -> PetVisualState.HAPPY
        PetEmotion.CURIOUS -> PetVisualState.CURIOUS
        PetEmotion.SLEEPY -> PetVisualState.SLEEPY
        PetEmotion.THINKING -> PetVisualState.THINKING
        PetEmotion.SAD -> PetVisualState.NEUTRAL
        PetEmotion.EXCITED -> PetVisualState.HAPPY
        PetEmotion.HUNGRY -> PetVisualState.NEUTRAL
    }
}
