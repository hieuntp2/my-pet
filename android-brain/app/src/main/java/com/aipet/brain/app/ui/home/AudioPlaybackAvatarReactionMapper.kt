package com.aipet.brain.app.ui.home

import com.aipet.brain.ui.avatar.model.AvatarEmotion
import java.util.Locale

internal object AudioPlaybackAvatarReactionMapper {
    fun toAvatarEmotion(category: String): AvatarEmotion? {
        return when (category.trim().uppercase(Locale.US)) {
            "ACKNOWLEDGMENT" -> AvatarEmotion.CURIOUS
            "CURIOUS" -> AvatarEmotion.CURIOUS
            "SURPRISED" -> AvatarEmotion.SURPRISED
            "HAPPY" -> AvatarEmotion.HAPPY
            else -> null
        }
    }
}
