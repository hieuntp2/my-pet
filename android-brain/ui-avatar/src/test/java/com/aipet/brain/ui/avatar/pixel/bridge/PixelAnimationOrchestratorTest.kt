package com.aipet.brain.ui.avatar.pixel.bridge

import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Neutral
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationClip
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationFrameEntry
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationPlaybackMode
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariantTier
import com.aipet.brain.ui.avatar.pixel.model.PixelFrame64
import com.aipet.brain.ui.avatar.pixel.model.PixelPetAnimationStateSet
import com.aipet.brain.ui.avatar.pixel.model.PixelPetDefaultPalette
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionStrategy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.random.Random

class PixelAnimationOrchestratorTest {

    @Test
    fun `default mapper converts avatar intents into visual states`() {
        val mapper = DefaultPixelPetStateMapper()

        assertEquals(Neutral, mapper.map(PixelPetBridgeState(PixelPetAvatarIntent.NEUTRAL)))
        assertEquals(Happy, mapper.map(PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED)))
    }

    @Test
    fun `orchestrator maps state to animation set and pushes selected clip into controller`() {
        val controller = PixelAnimationController()
        val happyVariantA = variant(id = "happy_a", paletteKey = PixelPetDefaultPalette.EyeBaseKey)
        val orchestrator = PixelAnimationOrchestrator(
            stateMapper = DefaultPixelPetStateMapper(),
            animationSetRegistry = registry(
                neutralVariants = listOf(variant(id = "neutral_a", paletteKey = PixelPetDefaultPalette.TransparentKey)),
                happyVariants = listOf(happyVariantA, variant(id = "happy_b", paletteKey = PixelPetDefaultPalette.HighlightKey))
            ),
            variantSelector = PixelAnimationVariantSelector(
                strategy = PixelVariantSelectionStrategy.SimpleRoundRobin,
                random = Random(0)
            ),
            animationController = controller
        )

        val selectedVariant = orchestrator.synchronize(
            PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED)
        )

        assertEquals(Happy, orchestrator.getActiveVisualState())
        assertSame(happyVariantA, selectedVariant)
        assertEquals("happy_a_clip", controller.getPlaybackState()?.clip?.id)
    }

    @Test
    fun `orchestrator does not reset the current clip while the same state is still playing`() {
        val controller = PixelAnimationController()
        val orchestrator = PixelAnimationOrchestrator(
            stateMapper = DefaultPixelPetStateMapper(),
            animationSetRegistry = registry(
                neutralVariants = listOf(loopVariant(id = "neutral_loop")),
                happyVariants = listOf(loopVariant(id = "happy_loop"))
            ),
            variantSelector = PixelAnimationVariantSelector(
                strategy = PixelVariantSelectionStrategy.SimpleRoundRobin,
                random = Random(1)
            ),
            animationController = controller
        )

        orchestrator.synchronize(PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED))
        controller.update(deltaMillis = 50)
        val playbackBefore = controller.getPlaybackState()

        val returnedVariant = orchestrator.synchronize(
            PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED)
        )
        val playbackAfter = controller.getPlaybackState()

        assertSame(orchestrator.getActiveVariant(), returnedVariant)
        assertEquals(playbackBefore?.clip?.id, playbackAfter?.clip?.id)
        assertEquals(playbackBefore?.elapsedFrameMillis, playbackAfter?.elapsedFrameMillis)
    }

    @Test
    fun `orchestrator selects the next variant when the current clip has finished`() {
        val controller = PixelAnimationController()
        val happyVariantA = oneShotVariant(id = "happy_a", paletteKey = PixelPetDefaultPalette.EyeBaseKey)
        val happyVariantB = oneShotVariant(id = "happy_b", paletteKey = PixelPetDefaultPalette.HighlightKey)
        val orchestrator = PixelAnimationOrchestrator(
            stateMapper = DefaultPixelPetStateMapper(),
            animationSetRegistry = registry(
                neutralVariants = listOf(oneShotVariant(id = "neutral_a", paletteKey = PixelPetDefaultPalette.TransparentKey)),
                happyVariants = listOf(happyVariantA, happyVariantB)
            ),
            variantSelector = PixelAnimationVariantSelector(
                strategy = PixelVariantSelectionStrategy.SimpleRoundRobin,
                random = Random(2)
            ),
            animationController = controller
        )

        val firstVariant = orchestrator.synchronize(
            PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED)
        )
        controller.update(deltaMillis = 120)
        val secondVariant = orchestrator.synchronize(
            PixelPetBridgeState(PixelPetAvatarIntent.ENGAGED)
        )

        assertSame(happyVariantA, firstVariant)
        assertSame(happyVariantB, secondVariant)
        assertEquals("happy_b_clip", controller.getPlaybackState()?.clip?.id)
    }

    private fun registry(
        neutralVariants: List<PixelAnimationVariant>,
        happyVariants: List<PixelAnimationVariant>
    ): PixelAnimationSetRegistry {
        return PixelAnimationSetRegistry(
            mapOf(
                Neutral to PixelPetAnimationStateSet(state = Neutral, variants = neutralVariants),
                Happy to PixelPetAnimationStateSet(state = Happy, variants = happyVariants)
            )
        )
    }

    private fun oneShotVariant(
        id: String,
        paletteKey: String
    ): PixelAnimationVariant {
        return variant(
            id = id,
            paletteKey = paletteKey,
            playbackMode = PixelAnimationPlaybackMode.ONE_SHOT
        )
    }

    private fun loopVariant(id: String): PixelAnimationVariant {
        return variant(
            id = id,
            paletteKey = PixelPetDefaultPalette.PupilKey,
            playbackMode = PixelAnimationPlaybackMode.LOOP
        )
    }

    private fun variant(
        id: String,
        paletteKey: String,
        playbackMode: PixelAnimationPlaybackMode = PixelAnimationPlaybackMode.LOOP
    ): PixelAnimationVariant {
        return PixelAnimationVariant(
            id = id,
            clip = PixelAnimationClip(
                id = "${id}_clip",
                playbackMode = playbackMode,
                frames = listOf(
                    PixelAnimationFrameEntry(
                        frame = solidFrame(paletteKey),
                        durationMillis = 120
                    )
                )
            ),
            tier = PixelAnimationVariantTier.PRIMARY,
            weight = 1
        )
    }

    private fun solidFrame(paletteKey: String): PixelFrame64 {
        val palette = PixelPetDefaultPalette.palette
        return PixelFrame64.filled(
            palette = palette,
            colorIndex = palette.indexOf(paletteKey)
        )
    }
}
