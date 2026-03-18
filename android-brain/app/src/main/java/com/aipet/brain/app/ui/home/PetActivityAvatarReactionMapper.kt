package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.ui.avatar.model.AvatarEmotion

internal object PetActivityAvatarReactionMapper {
    fun toAvatarEmotion(activityType: PetActivityType): AvatarEmotion {
        return when (activityType) {
            PetActivityType.FEED -> AvatarEmotion.HAPPY
            PetActivityType.PLAY -> AvatarEmotion.SURPRISED
            PetActivityType.REST -> AvatarEmotion.SLEEPY
        }
    }

    fun reactionDurationMs(activityType: PetActivityType): Long {
        return when (activityType) {
            PetActivityType.FEED -> 1_000L
            PetActivityType.PLAY -> 1_200L
            PetActivityType.REST -> 1_400L
        }
    }
}
