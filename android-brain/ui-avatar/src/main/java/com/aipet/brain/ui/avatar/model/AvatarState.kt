package com.aipet.brain.ui.avatar.model

data class AvatarState(
    val emotion: AvatarEmotion = AvatarEmotion.NEUTRAL,
    val eyeState: AvatarEyeState = AvatarEyeState.OPEN,
    val mouthState: AvatarMouthState = AvatarMouthState.NEUTRAL
)
