package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.model.AvatarEmotion

internal object BrainStateAvatarMapper {
    fun toAvatarEmotion(brainState: BrainState): AvatarEmotion {
        return when (brainState) {
            BrainState.IDLE -> AvatarEmotion.NEUTRAL
            BrainState.CURIOUS -> AvatarEmotion.CURIOUS
            BrainState.HAPPY -> AvatarEmotion.HAPPY
            BrainState.SLEEPY -> AvatarEmotion.SLEEPY
        }
    }
}
