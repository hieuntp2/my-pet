package com.aipet.brain.ui.avatar.pixel.bridge

import com.aipet.brain.ui.avatar.pixel.model.PixelAnimationVariant
import com.aipet.brain.ui.avatar.pixel.model.PixelPetVisualState
import com.aipet.brain.ui.avatar.pixel.playback.PixelAnimationController
import com.aipet.brain.ui.avatar.pixel.selection.PixelAnimationVariantSelector
import com.aipet.brain.ui.avatar.pixel.selection.PixelVariantSelectionContext

class PixelAnimationOrchestrator<T>(
    private val stateMapper: PixelPetStateMapper<T>,
    private val animationSetRegistry: PixelAnimationSetRegistry,
    private val variantSelector: PixelAnimationVariantSelector,
    private val animationController: PixelAnimationController
) {
    private val selectionContextsByState = mutableMapOf<PixelPetVisualState, PixelVariantSelectionContext>()
    private var activeVisualState: PixelPetVisualState? = null
    private var activeVariant: PixelAnimationVariant? = null

    fun synchronize(state: T): PixelAnimationVariant {
        val visualState = stateMapper.map(state)
        val shouldSelectNewVariant = visualState != activeVisualState ||
            activeVariant == null ||
            animationController.getPlaybackState() == null ||
            animationController.isFinished()

        if (!shouldSelectNewVariant) {
            return requireNotNull(activeVariant) {
                "Expected an active animation variant when no clip change is required."
            }
        }

        val animationSet = animationSetRegistry.requireAnimationSet(visualState)
        val selectionContext = selectionContextsByState.getOrPut(visualState) {
            PixelVariantSelectionContext()
        }
        val selectedVariant = variantSelector.selectNext(
            animationSet = animationSet,
            context = selectionContext
        )

        animationController.setClip(selectedVariant.clip)
        activeVisualState = visualState
        activeVariant = selectedVariant
        return selectedVariant
    }

    fun getActiveVisualState(): PixelPetVisualState? = activeVisualState

    fun getActiveVariant(): PixelAnimationVariant? = activeVariant
}
