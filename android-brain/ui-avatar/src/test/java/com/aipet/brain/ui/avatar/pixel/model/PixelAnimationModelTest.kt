package com.aipet.brain.ui.avatar.pixel.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PixelAnimationModelTest {

    @Test(expected = IllegalArgumentException::class)
    fun `pixel frame rejects malformed pixel counts`() {
        PixelFrame64(
            palette = PixelPetDefaultPalette.palette,
            pixelIndices = List(PixelFrame64.PIXEL_COUNT - 1) { 0 }
        )
    }

    @Test
    fun `sample clip can be created cleanly`() {
        val palette = PixelPetDefaultPalette.palette
        val transparent = palette.indexOf(PixelPetDefaultPalette.TransparentKey)
        val pupil = palette.indexOf(PixelPetDefaultPalette.PupilKey)
        val baseFrame = PixelFrame64.filled(palette = palette, colorIndex = transparent)
        val blinkFrame = PixelFrame64(
            palette = palette,
            pixelIndices = baseFrame.pixelIndices.toMutableList().apply {
                this[(32 * PixelFrame64.WIDTH) + 24] = pupil
                this[(32 * PixelFrame64.WIDTH) + 39] = pupil
            }
        )

        val clip = PixelAnimationClip(
            id = "neutral_blink",
            playbackMode = PixelAnimationPlaybackMode.LOOP,
            frames = listOf(
                PixelAnimationFrameEntry(frame = baseFrame, durationMillis = 180),
                PixelAnimationFrameEntry(frame = blinkFrame, durationMillis = 90)
            )
        )
        val variant = PixelAnimationVariant(
            id = "neutral_blink_primary",
            clip = clip,
            tier = PixelAnimationVariantTier.PRIMARY,
            weight = 3,
            categories = setOf("blink", "idle")
        )
        val stateSet = PixelPetAnimationStateSet(
            state = Neutral,
            variants = listOf(variant)
        )
        val catalog = PixelPetAnimationCatalog(
            states = mapOf(Neutral to stateSet)
        )

        assertEquals(270, clip.totalDurationMillis)
        assertEquals(1, stateSet.primaryVariants.size)
        assertTrue(catalog[Neutral]?.variants?.contains(variant) == true)
    }
}
