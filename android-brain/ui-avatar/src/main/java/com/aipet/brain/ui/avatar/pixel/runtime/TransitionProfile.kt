package com.aipet.brain.ui.avatar.pixel.runtime

/**
 * Per-layer transition speeds in units-per-millisecond.
 * A speed of 0.004 means 250 ms to traverse the full 0..1 range.
 */
internal data class TransitionProfile(
    val lidSpeed: Float,
    val gazeSpeed: Float,
    val browSpeed: Float,
    val warmthSpeed: Float = lidSpeed
) {
    companion object {
        /** Default smooth interpolation — natural, organic feel. */
        val SMOOTH = TransitionProfile(lidSpeed = 0.004f, gazeSpeed = 0.010f, browSpeed = 0.005f)

        /** Fast intentional look — gaze snaps, lids adjust quickly. */
        val SNAP = TransitionProfile(lidSpeed = 0.08f, gazeSpeed = 0.10f, browSpeed = 0.08f)

        /** Sleepy drift — very gradual, heavy-lidded. */
        val SLEEPY_DRIFT = TransitionProfile(lidSpeed = 0.0018f, gazeSpeed = 0.003f, browSpeed = 0.002f)

        /** Instant startle — near-frame-perfect. */
        val STARTLED_CUT = TransitionProfile(lidSpeed = 0.20f, gazeSpeed = 0.20f, browSpeed = 0.20f)

        /** Playful pop — snappy but not stabbing. */
        val PLAYFUL_POP = TransitionProfile(lidSpeed = 0.055f, gazeSpeed = 0.07f, browSpeed = 0.055f)
    }
}
