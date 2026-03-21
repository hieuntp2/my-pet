package com.aipet.brain.ui.avatar.pixel.mock

import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariantTier
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import com.aipet.brain.ui.avatar.pixel.model.Sleepy
import com.aipet.brain.ui.avatar.pixel.model.Thinking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MockPixelPetAnimationPackTest {

    @Test
    fun `mock pack registers the expected home demo visual states`() {
        val registry = MockPixelPetAnimationPack.createRegistry()

        assertNotNull(registry[Neutral])
        assertNotNull(registry[Happy])
        assertNotNull(registry[Curious])
        assertNotNull(registry[Thinking])
        assertNotNull(registry[Sleepy])
    }

    @Test
    fun `neutral state registers the four canonical idle variants`() {
        val neutralVariants = MockPixelPetAnimationPack.createRegistry()
            .requireAnimationSet(Neutral)
            .variants

        assertEquals(
            listOf(
                "Neutral_A_SlowBlink",
                "Neutral_B_GlanceLeft",
                "Neutral_C_GlanceRight",
                "Neutral_D_DoubleBlink"
            ),
            neutralVariants.map { it.id }
        )
        assertEquals(PixelAnimationVariantTier.RARE, neutralVariants.last().tier)
    }

    @Test
    fun `happy state registers the authored positive variants`() {
        val happyVariants = MockPixelPetAnimationPack.createRegistry()
            .requireAnimationSet(Happy)
            .variants

        assertEquals(
            listOf(
                "Happy_A_SoftSquint",
                "Happy_B_OpenBounce",
                "Happy_C_WinkAsymmetry",
                "Happy_D_BrightOpen"
            ),
            happyVariants.map { it.id }
        )
        assertEquals(PixelAnimationVariantTier.RARE, happyVariants.last().tier)
    }

    @Test
    fun `curious state registers the authored attentive variants`() {
        val curiousVariants = MockPixelPetAnimationPack.createRegistry()
            .requireAnimationSet(Curious)
            .variants

        assertEquals(
            listOf(
                "Curious_A_LookLeft",
                "Curious_B_LookRight",
                "Curious_C_FocusSquint",
                "Curious_D_WidenThenSettle"
            ),
            curiousVariants.map { it.id }
        )
        assertEquals(PixelAnimationVariantTier.RARE, curiousVariants.last().tier)
    }

    @Test
    fun `neutral variants share the same canonical open base frame`() {
        val neutralVariants = MockPixelPetAnimationPack.createRegistry()
            .requireAnimationSet(Neutral)
            .variants
        val canonicalOpenFrame = neutralVariants.first().clip.frames.first().frame

        neutralVariants.forEach { variant ->
            assertEquals(canonicalOpenFrame, variant.clip.frames.first().frame)
        }
    }

    @Test
    fun `happy and curious preserve the neutral eye shell bounds`() {
        val registry = MockPixelPetAnimationPack.createRegistry()
        val neutralFrame = registry.requireAnimationSet(Neutral).variants.first().clip.frames.first().frame
        val happyFrame = registry.requireAnimationSet(Happy).variants.first().clip.frames.first().frame
        val curiousFrame = registry.requireAnimationSet(Curious).variants.first().clip.frames.first().frame

        assertEquals(eyeBaseBounds(neutralFrame), eyeBaseBounds(happyFrame))
        assertEquals(eyeBaseBounds(neutralFrame), eyeBaseBounds(curiousFrame))
    }

    @Test
    fun `happy and curious first frames are visibly distinct from neutral`() {
        val registry = MockPixelPetAnimationPack.createRegistry()
        val neutralFrame = registry.requireAnimationSet(Neutral).variants.first().clip.frames.first().frame
        val happyFrame = registry.requireAnimationSet(Happy).variants.first().clip.frames.first().frame
        val curiousFrame = registry.requireAnimationSet(Curious).variants.first().clip.frames.first().frame

        assertNotEquals(neutralFrame, happyFrame)
        assertNotEquals(neutralFrame, curiousFrame)
    }

    @Test
    fun `expressive variants use timing and asymmetry for variation`() {
        val registry = MockPixelPetAnimationPack.createRegistry()
        val happyVariants = registry.requireAnimationSet(Happy).variants
        val curiousVariants = registry.requireAnimationSet(Curious).variants

        assertTrue(happyVariants[2].clip.frames.size >= 4)
        assertTrue(curiousVariants[2].clip.totalDurationMillis < curiousVariants[3].clip.totalDurationMillis)
        assertTrue(hasAsymmetricFrame(happyVariants[2]))
        assertNotEquals(curiousVariants[0].clip.frames[1].frame, curiousVariants[1].clip.frames[1].frame)
    }

    private fun eyeBaseBounds(frame: PixelFrame64): IntRangeSummary {
        val eyeBaseIndex = PixelPetDefaultPalette.palette.indexOf(PixelPetDefaultPalette.EyeBaseKey)
        var minX = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var minY = Int.MAX_VALUE
        var maxY = Int.MIN_VALUE

        for (y in 0 until PixelFrame64.HEIGHT) {
            for (x in 0 until PixelFrame64.WIDTH) {
                if (frame.colorIndexAt(x = x, y = y) == eyeBaseIndex) {
                    minX = minOf(minX, x)
                    maxX = maxOf(maxX, x)
                    minY = minOf(minY, y)
                    maxY = maxOf(maxY, y)
                }
            }
        }

        return IntRangeSummary(minX = minX, maxX = maxX, minY = minY, maxY = maxY)
    }

    private fun hasAsymmetricFrame(variant: PixelAnimationVariant): Boolean {
        val pupilIndex = PixelPetDefaultPalette.palette.indexOf(PixelPetDefaultPalette.PupilKey)
        return variant.clip.frames.any { frameEntry ->
            val frame = frameEntry.frame
            val leftPupilCount = countColor(frame = frame, xRange = 12..26, colorIndex = pupilIndex)
            val rightPupilCount = countColor(frame = frame, xRange = 37..51, colorIndex = pupilIndex)
            leftPupilCount != rightPupilCount
        }
    }

    private fun countColor(
        frame: PixelFrame64,
        xRange: IntRange,
        colorIndex: Int
    ): Int {
        var count = 0
        for (y in 0 until PixelFrame64.HEIGHT) {
            for (x in xRange) {
                if (frame.colorIndexAt(x = x, y = y) == colorIndex) {
                    count += 1
                }
            }
        }
        return count
    }

    private data class IntRangeSummary(
        val minX: Int,
        val maxX: Int,
        val minY: Int,
        val maxY: Int
    )
}
