package com.aipet.brain.ui.avatar.pixel.animation

import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Input context for the FaceAnimationRuntime.
 *
 * Drives idle family selection and state leakage (energy, hunger, etc. → continuous
 * animation modifiers). Passed each frame from the composable.
 */
data class FaceAnimationContext(
    val intent: PixelPetAvatarIntent = PixelPetAvatarIntent.NEUTRAL,
    /** Pet energy level 0–100. Low energy → slower transitions. */
    val energy: Int = 100,
    /** Pet hunger level 0–100. High hunger → concerned brow, scan behaviour. */
    val hunger: Int = 0,
    /** Pet sleepiness 0–100. High sleepiness → drooped lids, slow gaze, SLEEPY family. */
    val sleepiness: Int = 0,
    /** Pet social need 0–100. Low social → EXPECTANT family. */
    val social: Int = 50,
    /** Bond level 0–100. High bond → warmer eyebrow colour in reactions. */
    val bond: Int = 50
)
