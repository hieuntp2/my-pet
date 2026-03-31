package com.aipet.brain.ui.avatar.pixel.runtime

/**
 * A single named beat within an idle family that sets layer targets for a random duration.
 * The IdleDirector picks beats one at a time with anti-repeat logic and weighted randomness.
 */
internal data class IdleBeat(
    val id: String,
    val family: IdleBehaviorFamily,
    val weight: Float,
    val holdDurationRange: LongRange,
    val targets: LayerTargets,
    val transitionProfile: TransitionProfile = TransitionProfile.SMOOTH,
    val cooldownMs: Long = 2800L,
    val isRare: Boolean = false
)
