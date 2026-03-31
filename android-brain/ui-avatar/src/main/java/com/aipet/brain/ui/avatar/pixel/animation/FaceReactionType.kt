package com.aipet.brain.ui.avatar.pixel.animation

/**
 * All reaction types that the FaceAnimationRuntime can execute as a full
 * attention → anticipation → main → after-effect → settle sequence.
 */
enum class FaceReactionType {
    TAP_ENGAGED,
    TAP_PLAYFUL,
    TAP_ANNOYED,
    LONG_PRESS_CUDDLE,
    LONG_PRESS_SLEEPY,
    EXCITED_GREETING,
    STARTLED_SNAP,
    KEYWORD_ATTENTIVE,
    LOUD_SOUND_STARTLED,
    GAME_CELEBRATE,
    GAME_FAIL
}
