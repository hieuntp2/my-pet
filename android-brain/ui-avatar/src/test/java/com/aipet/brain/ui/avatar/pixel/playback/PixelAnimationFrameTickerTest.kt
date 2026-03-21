package com.aipet.brain.ui.avatar.pixel.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class PixelAnimationFrameTickerTest {
    @Test
    fun `pause suppresses delta and resume continues from latest frame time`() {
        val ticker = PixelAnimationFrameTicker()

        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 1_000_000L, isPlaying = true, speedMultiplier = 1f))
        assertEquals(16L, ticker.nextDeltaMillis(frameTimeNanos = 17_000_000L, isPlaying = true, speedMultiplier = 1f))
        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 33_000_000L, isPlaying = false, speedMultiplier = 1f))
        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 49_000_000L, isPlaying = false, speedMultiplier = 1f))
        assertEquals(16L, ticker.nextDeltaMillis(frameTimeNanos = 65_000_000L, isPlaying = true, speedMultiplier = 1f))
    }

    @Test
    fun `speed multiplier scales only future deltas`() {
        val ticker = PixelAnimationFrameTicker()

        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 1_000_000L, isPlaying = true, speedMultiplier = 1f))
        assertEquals(16L, ticker.nextDeltaMillis(frameTimeNanos = 17_000_000L, isPlaying = true, speedMultiplier = 1f))
        assertEquals(32L, ticker.nextDeltaMillis(frameTimeNanos = 33_000_000L, isPlaying = true, speedMultiplier = 2f))
        assertEquals(8L, ticker.nextDeltaMillis(frameTimeNanos = 49_000_000L, isPlaying = true, speedMultiplier = 0.5f))
    }

    @Test
    fun `reset clears prior frame timing`() {
        val ticker = PixelAnimationFrameTicker()

        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 1_000_000L, isPlaying = true, speedMultiplier = 1f))
        assertEquals(16L, ticker.nextDeltaMillis(frameTimeNanos = 17_000_000L, isPlaying = true, speedMultiplier = 1f))

        ticker.reset()

        assertEquals(0L, ticker.nextDeltaMillis(frameTimeNanos = 33_000_000L, isPlaying = true, speedMultiplier = 1f))
    }
}
