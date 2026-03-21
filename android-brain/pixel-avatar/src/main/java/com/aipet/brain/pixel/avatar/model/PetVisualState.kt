package com.aipet.brain.pixel.avatar.model

/**
 * Visual state vocabulary for the eye-only pixel pet animation system.
 *
 * These are animation-facing states, not brain-layer states. They are mapped
 * from the app's PetEmotion via an adapter in the app module.
 */
enum class PetVisualState {
    NEUTRAL,
    HAPPY,
    CURIOUS,
    SLEEPY,
    THINKING
}
