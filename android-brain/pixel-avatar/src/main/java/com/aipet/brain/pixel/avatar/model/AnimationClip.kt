package com.aipet.brain.pixel.avatar.model

/**
 * One frame inside an animation clip with its display hold duration.
 *
 * @param frame  The pixel content to display.
 * @param holdMs How long this frame should stay on screen in milliseconds.
 */
data class AnimationFrameEntry(
    val frame: PixelFrame64,
    val holdMs: Long
)

/**
 * A sequence of frames that forms one animation clip.
 *
 * @param frames  Ordered list of frame entries.
 * @param loop    If true the clip restarts after the last frame. If false it
 *                holds the last frame and signals completion to the controller.
 * @param name    Human-readable identifier for debugging and preview tooling.
 */
data class AnimationClip(
    val frames: List<AnimationFrameEntry>,
    val loop: Boolean,
    val name: String
) {
    init {
        require(frames.isNotEmpty()) { "AnimationClip '$name' must have at least one frame" }
    }
}

/**
 * One variant within a state's animation set.
 *
 * @param clip    The clip to play when this variant is selected.
 * @param weight  Relative selection weight. Higher weight = more frequent.
 *                All weights in a set are summed to form a probability distribution.
 * @param name    Human-readable variant label for preview/debug.
 */
data class AnimationVariant(
    val clip: AnimationClip,
    val weight: Float,
    val name: String
) {
    init {
        require(weight > 0f) { "AnimationVariant '$name' weight must be positive, got $weight" }
    }
}

/**
 * The full animation set for one [PetVisualState]: 3–4 variants with weights.
 *
 * The controller consults this set to perform weighted random variant selection.
 *
 * @param state     The pet visual state this set belongs to.
 * @param variants  List of 3–4 variants. Must be non-empty.
 */
data class AnimationStateSet(
    val state: PetVisualState,
    val variants: List<AnimationVariant>
) {
    init {
        require(variants.isNotEmpty()) { "AnimationStateSet for $state must have at least one variant" }
    }
}
