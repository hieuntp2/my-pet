package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.ui.avatar.model.AvatarEmotion

internal object PetEmotionAvatarMapper {
    fun toAvatarEmotion(petEmotion: PetEmotion): AvatarEmotion {
        return when (petEmotion) {
            PetEmotion.IDLE -> AvatarEmotion.NEUTRAL
            PetEmotion.HAPPY -> AvatarEmotion.HAPPY
            PetEmotion.CURIOUS -> AvatarEmotion.CURIOUS
            PetEmotion.SLEEPY -> AvatarEmotion.SLEEPY
            PetEmotion.SAD -> AvatarEmotion.SLEEPY
            PetEmotion.EXCITED -> AvatarEmotion.SURPRISED
            PetEmotion.HUNGRY -> AvatarEmotion.CURIOUS
            PetEmotion.THINKING -> AvatarEmotion.NEUTRAL
            PetEmotion.RELIEVED -> AvatarEmotion.NEUTRAL
            PetEmotion.NEEDY -> AvatarEmotion.SLEEPY
            PetEmotion.DISTANT -> AvatarEmotion.NEUTRAL
            PetEmotion.WITHDRAWN -> AvatarEmotion.SLEEPY
            PetEmotion.STARTLED -> AvatarEmotion.SURPRISED
            PetEmotion.SHY -> AvatarEmotion.NEUTRAL
        }
    }
}
