package com.aipet.brain.ui.avatar.pixel.runtime

/** High-level behavior families driving the idle animation loop. */
internal enum class IdleBehaviorFamily {
    CALM,
    CURIOUS,
    SLEEPY,
    EXPECTANT,
    PLAYFUL
}

/** Blink types for the IndependentBlinkLayer. */
internal enum class BlinkType {
    NORMAL,
    DOUBLE,
    SLEEPY,
    STARTLED,
    ASYMMETRIC_LEFT_LEAD,
    ASYMMETRIC_RIGHT_LEAD
}

/** Main animation phase: idle beat or active reaction. */
internal enum class AnimationPhase {
    IDLE,
    REACTION
}
