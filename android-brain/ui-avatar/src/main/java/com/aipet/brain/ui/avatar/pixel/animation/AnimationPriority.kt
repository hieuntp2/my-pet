package com.aipet.brain.ui.avatar.pixel.animation

/**
 * Priority levels for FaceAnimationRuntime reaction triggering.
 * Higher priority interrupts lower priority.
 */
object AnimationPriority {
    const val IDLE = 0
    const val AUDIO = 1
    const val INTERACTION = 2
    const val GREETING = 3
    const val STARTLED = 4
}
