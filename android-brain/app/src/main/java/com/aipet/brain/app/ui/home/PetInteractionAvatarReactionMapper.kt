package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.ui.avatar.model.AvatarEmotion

internal object PetInteractionAvatarReactionMapper {
    fun toAvatarEmotion(interactionType: PetInteractionType): AvatarEmotion {
        return when (interactionType) {
            PetInteractionType.TAP -> AvatarEmotion.HAPPY
            PetInteractionType.LONG_PRESS -> AvatarEmotion.SURPRISED
        }
    }

    fun reactionDurationMs(interactionType: PetInteractionType): Long {
        return when (interactionType) {
            PetInteractionType.TAP -> 700L
            PetInteractionType.LONG_PRESS -> 1_200L
        }
    }
}
