package com.aipet.brain.pixel.avatar.clips

import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Central registry: maps each [PetVisualState] to its [AnimationStateSet].
 *
 * Used by [PixelAnimationController] on construction and by [PixelPetAnimator] in the app layer.
 */
object ClipRegistry {
    val ALL_STATES: Map<PetVisualState, AnimationStateSet> = mapOf(
        PetVisualState.NEUTRAL to NeutralStateClips.STATE_SET,
        PetVisualState.HAPPY to HappyStateClips.STATE_SET,
        PetVisualState.CURIOUS to CuriousStateClips.STATE_SET,
        PetVisualState.SLEEPY to SleepyStateClips.STATE_SET,
        PetVisualState.THINKING to ThinkingStateClips.STATE_SET
    )
}
