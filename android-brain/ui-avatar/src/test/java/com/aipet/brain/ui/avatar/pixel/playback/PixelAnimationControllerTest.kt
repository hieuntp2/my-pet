package com.aipet.brain.ui.avatar.pixel.playback

import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationFrameEntry
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelAnimationControllerTest {

    @Test
    fun `update advances through frames using per-frame durations`() {
        val clip = sampleClip(playbackMode = PixelAnimationPlaybackMode.LOOP)
        val controller = PixelAnimationController()

        controller.setClip(clip)
        controller.update(deltaMillis = 90)
        assertSame(clip.frames[0].frame, controller.getCurrentFrame())

        controller.update(deltaMillis = 20)
        assertSame(clip.frames[1].frame, controller.getCurrentFrame())

        controller.update(deltaMillis = 170)
        assertSame(clip.frames[2].frame, controller.getCurrentFrame())
    }

    @Test
    fun `loop clips wrap back to the first frame when delta crosses the end`() {
        val clip = sampleClip(playbackMode = PixelAnimationPlaybackMode.LOOP)
        val controller = PixelAnimationController()

        controller.setClip(clip)
        controller.update(deltaMillis = 360)

        assertSame(clip.frames[0].frame, controller.getCurrentFrame())
        assertFalse(controller.isFinished())
        assertEquals(0, controller.getPlaybackState()?.frameIndex)
        assertEquals(10L, controller.getPlaybackState()?.elapsedFrameMillis)
    }

    @Test
    fun `one-shot clips stop on the final frame and mark playback complete`() {
        val clip = sampleClip(playbackMode = PixelAnimationPlaybackMode.ONE_SHOT)
        val controller = PixelAnimationController()

        controller.setClip(clip)
        controller.update(deltaMillis = 1000)

        assertSame(clip.frames.last().frame, controller.getCurrentFrame())
        assertTrue(controller.isFinished())
        assertEquals(clip.frames.lastIndex, controller.getPlaybackState()?.frameIndex)
        assertEquals(
            clip.frames.last().durationMillis.toLong(),
            controller.getPlaybackState()?.elapsedFrameMillis
        )

        controller.update(deltaMillis = 50)
        assertSame(clip.frames.last().frame, controller.getCurrentFrame())
        assertTrue(controller.isFinished())
    }

    @Test
    fun `setClip resets playback state for a new clip`() {
        val firstClip = sampleClip(playbackMode = PixelAnimationPlaybackMode.LOOP)
        val secondClip = PixelAnimationClip(
            id = "thinking_hold",
            playbackMode = PixelAnimationPlaybackMode.ONE_SHOT,
            frames = listOf(
                PixelAnimationFrameEntry(frame = solidFrame(PixelPetDefaultPalette.HighlightKey), durationMillis = 120),
                PixelAnimationFrameEntry(frame = solidFrame(PixelPetDefaultPalette.AccentKey), durationMillis = 120)
            )
        )
        val controller = PixelAnimationController()

        controller.setClip(firstClip)
        controller.update(deltaMillis = 140)
        controller.setClip(secondClip)

        assertSame(secondClip.frames.first().frame, controller.getCurrentFrame())
        assertEquals(0, controller.getPlaybackState()?.frameIndex)
        assertEquals(0L, controller.getPlaybackState()?.elapsedFrameMillis)
        assertFalse(controller.isFinished())
    }


    @Test
    fun `setClipIfDifferent skips redundant clip activations but restart stays explicit`() {
        val clip = sampleClip(playbackMode = PixelAnimationPlaybackMode.LOOP)
        val controller = PixelAnimationController()

        controller.setClip(clip)
        val changed = controller.setClipIfDifferent(clip)
        controller.setClip(clip)

        val diagnostics = controller.getDiagnostics()
        assertFalse(changed)
        assertEquals(2, diagnostics.clipActivationCount)
        assertEquals(1, diagnostics.clipRestartCount)
        assertEquals(listOf("neutral_blink", "neutral_blink"), diagnostics.recentClipHistory)
    }

    @Test
    fun `clock-driven updates compute delta from successive timestamps`() {
        val clip = sampleClip(playbackMode = PixelAnimationPlaybackMode.LOOP)
        val controller = PixelAnimationController()
        val clock = FakePixelAnimationClock()

        controller.setClip(clip)
        clock.nowMillis = 1_000L
        controller.update(clock)
        assertSame(clip.frames[0].frame, controller.getCurrentFrame())

        clock.nowMillis = 1_110L
        controller.update(clock)
        assertSame(clip.frames[1].frame, controller.getCurrentFrame())

        clock.nowMillis = 1_260L
        controller.update(clock)
        assertSame(clip.frames[2].frame, controller.getCurrentFrame())
    }

    private fun sampleClip(playbackMode: PixelAnimationPlaybackMode): PixelAnimationClip {
        return PixelAnimationClip(
            id = "neutral_blink",
            playbackMode = playbackMode,
            frames = listOf(
                PixelAnimationFrameEntry(frame = solidFrame(PixelPetDefaultPalette.TransparentKey), durationMillis = 100),
                PixelAnimationFrameEntry(frame = solidFrame(PixelPetDefaultPalette.EyeBaseKey), durationMillis = 150),
                PixelAnimationFrameEntry(frame = solidFrame(PixelPetDefaultPalette.PupilKey), durationMillis = 100)
            )
        )
    }

    private fun solidFrame(paletteKey: String): PixelFrame64 {
        val palette = PixelPetDefaultPalette.palette
        return PixelFrame64.filled(
            palette = palette,
            colorIndex = palette.indexOf(paletteKey)
        )
    }

    private class FakePixelAnimationClock : PixelAnimationClock {
        var nowMillis: Long = 0L

        override fun nowMillis(): Long = nowMillis
    }
}
