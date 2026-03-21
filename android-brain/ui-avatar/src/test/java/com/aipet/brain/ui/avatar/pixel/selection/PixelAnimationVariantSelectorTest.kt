package com.aipet.brain.ui.avatar.pixel.selection

import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationFrameEntry
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariantTier
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetAnimationStateSet
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class PixelAnimationVariantSelectorTest {

    @Test
    fun `weighted random selection favors higher weight variants`() {
        val animationSet = animationSetOf(
            variant(id = "common", weight = 1),
            variant(id = "frequent", weight = 4)
        )
        val selector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(nonRepeatingHistorySize = 0),
            random = Random(1234)
        )
        var commonCount = 0
        var frequentCount = 0

        repeat(SELECTION_ITERATIONS) {
            val selectedVariant = selector.selectNext(
                animationSet = animationSet,
                context = PixelVariantSelectionContext()
            )
            when (selectedVariant.id) {
                "common" -> commonCount += 1
                "frequent" -> frequentCount += 1
            }
        }

        assertTrue(frequentCount > commonCount)
    }

    @Test
    fun `weighted random strategy avoids selecting the same variant twice in a row by default`() {
        val animationSet = animationSetOf(
            variant(id = "blink_a", weight = 1),
            variant(id = "blink_b", weight = 1)
        )
        val context = PixelVariantSelectionContext()
        val selector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(),
            random = Random(0)
        )

        val firstSelection = selector.selectNext(animationSet = animationSet, context = context)
        val secondSelection = selector.selectNext(animationSet = animationSet, context = context)

        assertNotEquals(firstSelection.id, secondSelection.id)
        assertEquals(listOf(firstSelection.id, secondSelection.id), context.recentVariantIds)
    }

    @Test
    fun `weighted random strategy can avoid the last two variants when enough options exist`() {
        val animationSet = animationSetOf(
            variant(id = "a", weight = 1),
            variant(id = "b", weight = 1),
            variant(id = "c", weight = 1)
        )
        val context = PixelVariantSelectionContext(recentVariantIds = listOf("a", "b"))
        val selector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(nonRepeatingHistorySize = 2),
            random = Random(5)
        )

        val selectedVariant = selector.selectNext(animationSet = animationSet, context = context)

        assertEquals("c", selectedVariant.id)
        assertEquals(listOf("a", "b", "c"), context.recentVariantIds)
    }

    @Test
    fun `selector falls back to the only variant when animation set has one option`() {
        val onlyVariant = variant(id = "single_idle", weight = 5)
        val animationSet = animationSetOf(onlyVariant)
        val context = PixelVariantSelectionContext()
        val selector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(),
            random = Random(99)
        )

        val firstSelection = selector.selectNext(animationSet = animationSet, context = context)
        val secondSelection = selector.selectNext(animationSet = animationSet, context = context)

        assertEquals(onlyVariant.id, firstSelection.id)
        assertEquals(onlyVariant.id, secondSelection.id)
        assertEquals(listOf(onlyVariant.id, onlyVariant.id), context.recentVariantIds)
    }

    @Test
    fun `selectors with the same seed produce the same weighted sequence`() {
        val animationSet = animationSetOf(
            variant(id = "a", weight = 1),
            variant(id = "b", weight = 2),
            variant(id = "c", weight = 3)
        )
        val firstSelector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(nonRepeatingHistorySize = 0),
            random = Random(777)
        )
        val secondSelector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.WeightedRandom(nonRepeatingHistorySize = 0),
            random = Random(777)
        )

        val firstSequence = List(6) {
            firstSelector.selectNext(
                animationSet = animationSet,
                context = PixelVariantSelectionContext()
            ).id
        }
        val secondSequence = List(6) {
            secondSelector.selectNext(
                animationSet = animationSet,
                context = PixelVariantSelectionContext()
            ).id
        }

        assertEquals(firstSequence, secondSequence)
    }

    @Test
    fun `selection context keeps only the three most recent variants`() {
        val context = PixelVariantSelectionContext()

        context.recordSelection("v1")
        context.recordSelection("v2")
        context.recordSelection("v3")
        context.recordSelection("v4")

        assertEquals(listOf("v2", "v3", "v4"), context.recentVariantIds)
        assertEquals("v4", context.lastVariantId)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `selection context rejects blank ids during construction`() {
        PixelVariantSelectionContext(recentVariantIds = listOf("valid", " "))
    }

    @Test
    fun `simple round robin walks variants in order`() {
        val animationSet = animationSetOf(
            variant(id = "one", weight = 1),
            variant(id = "two", weight = 1),
            variant(id = "three", weight = 1)
        )
        val context = PixelVariantSelectionContext()
        val selector = PixelAnimationVariantSelector(
            strategy = PixelVariantSelectionStrategy.SimpleRoundRobin,
            random = Random(1)
        )

        val sequence = List(4) {
            selector.selectNext(animationSet = animationSet, context = context).id
        }

        assertEquals(listOf("one", "two", "three", "one"), sequence)
    }

    private fun animationSetOf(vararg variants: PixelAnimationVariant): PixelPetAnimationStateSet {
        return PixelPetAnimationStateSet(
            state = Curious,
            variants = variants.toList()
        )
    }

    private fun variant(
        id: String,
        weight: Int
    ): PixelAnimationVariant {
        return PixelAnimationVariant(
            id = id,
            clip = PixelAnimationClip(
                id = "${id}_clip",
                playbackMode = PixelAnimationPlaybackMode.LOOP,
                frames = listOf(
                    PixelAnimationFrameEntry(
                        frame = solidFrame(PixelPetDefaultPalette.PupilKey),
                        durationMillis = 120
                    )
                )
            ),
            tier = PixelAnimationVariantTier.PRIMARY,
            weight = weight
        )
    }

    private fun solidFrame(paletteKey: String): PixelFrame64 {
        val palette = PixelPetDefaultPalette.palette
        return PixelFrame64.filled(
            palette = palette,
            colorIndex = palette.indexOf(paletteKey)
        )
    }

    private companion object {
        private const val SELECTION_ITERATIONS = 200
    }
}
