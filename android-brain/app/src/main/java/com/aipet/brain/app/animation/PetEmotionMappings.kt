package com.aipet.brain.app.animation

import com.aipet.brain.app.ui.audio.model.AudioCategory
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.pixel.avatar.model.PetVisualState
import com.aipet.brain.ui.avatar.model.AvatarEmotion

/**
 * Canonical emotion mapping table shared by animation adapters.
 *
 * The mappings preserve the existing behavior and naming contracts while avoiding
 * duplicated `when` branches across app animation implementations.
 */
object PetEmotionMappings {
    fun petToAnimationEmotion(emotion: PetEmotion): PetAnimationEmotion {
        return when (emotion) {
            PetEmotion.IDLE -> PetAnimationEmotion.CALM
            PetEmotion.HAPPY -> PetAnimationEmotion.HAPPY
            PetEmotion.CURIOUS -> PetAnimationEmotion.CURIOUS
            PetEmotion.SLEEPY -> PetAnimationEmotion.SLEEPY
            PetEmotion.SAD -> PetAnimationEmotion.SAD
            PetEmotion.EXCITED -> PetAnimationEmotion.EXCITED
            PetEmotion.HUNGRY -> PetAnimationEmotion.HUNGRY
            PetEmotion.THINKING -> PetAnimationEmotion.CALM
        }
    }

    fun animationToPetEmotion(emotion: PetAnimationEmotion): PetEmotion {
        return when (emotion) {
            PetAnimationEmotion.CALM -> PetEmotion.IDLE
            PetAnimationEmotion.HAPPY -> PetEmotion.HAPPY
            PetAnimationEmotion.CURIOUS -> PetEmotion.CURIOUS
            PetAnimationEmotion.SLEEPY -> PetEmotion.SLEEPY
            PetAnimationEmotion.SAD -> PetEmotion.SAD
            PetAnimationEmotion.EXCITED -> PetEmotion.EXCITED
            PetAnimationEmotion.HUNGRY -> PetEmotion.HUNGRY
        }
    }

    fun animationToAvatarEmotion(emotion: PetAnimationEmotion): AvatarEmotion {
        return when (emotion) {
            PetAnimationEmotion.CALM -> AvatarEmotion.NEUTRAL
            PetAnimationEmotion.HAPPY -> AvatarEmotion.HAPPY
            PetAnimationEmotion.CURIOUS -> AvatarEmotion.CURIOUS
            PetAnimationEmotion.SLEEPY -> AvatarEmotion.SLEEPY
            PetAnimationEmotion.SAD -> AvatarEmotion.SLEEPY
            PetAnimationEmotion.EXCITED -> AvatarEmotion.SURPRISED
            PetAnimationEmotion.HUNGRY -> AvatarEmotion.CURIOUS
        }
    }

    fun petToVisualState(emotion: PetEmotion): PetVisualState {
        return when (emotion) {
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

    fun audioToAnimationEmotion(category: AudioCategory): PetAnimationEmotion {
        return when (category) {
            AudioCategory.ACKNOWLEDGMENT,
            AudioCategory.GREETING,
            AudioCategory.HAPPY -> PetAnimationEmotion.HAPPY
            AudioCategory.CURIOUS -> PetAnimationEmotion.CURIOUS
            AudioCategory.SLEEPY -> PetAnimationEmotion.SLEEPY
            AudioCategory.SURPRISED -> PetAnimationEmotion.EXCITED
            AudioCategory.WARNING_NO -> PetAnimationEmotion.SAD
        }
    }

    fun audioToPetEmotion(category: AudioCategory): PetEmotion {
        return animationToPetEmotion(audioToAnimationEmotion(category))
    }

    fun audioToPetEmotionOrNull(category: AudioCategory): PetEmotion? {
        return when (category) {
            AudioCategory.ACKNOWLEDGMENT,
            AudioCategory.GREETING,
            AudioCategory.HAPPY,
            AudioCategory.CURIOUS,
            AudioCategory.SLEEPY -> audioToPetEmotion(category)
            AudioCategory.SURPRISED,
            AudioCategory.WARNING_NO -> null
        }
    }
}
