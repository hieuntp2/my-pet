package com.aipet.brain.ui.avatar.pixel.playback

import kotlin.math.roundToLong

class PixelAnimationFrameTicker {
    private var lastFrameTimeNanos: Long? = null

    fun nextDeltaMillis(
        frameTimeNanos: Long,
        isPlaying: Boolean,
        speedMultiplier: Float
    ): Long {
        require(speedMultiplier > 0f) { "speedMultiplier must be greater than 0." }

        val previousFrameTimeNanos = lastFrameTimeNanos
        lastFrameTimeNanos = frameTimeNanos

        if (!isPlaying || previousFrameTimeNanos == null) {
            return 0L
        }

        val rawDeltaMillis = (frameTimeNanos - previousFrameTimeNanos) / NANOS_PER_MILLISECOND.toDouble()
        return (rawDeltaMillis * speedMultiplier)
            .roundToLong()
            .coerceAtLeast(0L)
    }

    fun reset() {
        lastFrameTimeNanos = null
    }

    private companion object {
        private const val NANOS_PER_MILLISECOND = 1_000_000L
    }
}
