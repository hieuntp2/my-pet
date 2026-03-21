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
    private val recentTransitions = mutableListOf<PixelAnimationTransitionRecord>()
    private var activeVisualState: PixelPetVisualState? = null
    private var activeVariant: PixelAnimationVariant? = null
    private var synchronizationCount: Int = 0
    private var stateTransitionCount: Int = 0
    private var clipActivationCount: Int = 0
    private var clipReuseCount: Int = 0
    private var clipResyncCount: Int = 0

    fun synchronize(state: T): PixelAnimationVariant {
        synchronizationCount += 1
        val visualState = stateMapper.map(state)
        val playbackState = animationController.getPlaybackState()
        val currentVariant = activeVariant
        val visualStateChanged = visualState != activeVisualState

        if (visualStateChanged && activeVisualState != null) {
            stateTransitionCount += 1
        }

        if (
            !visualStateChanged &&
            currentVariant != null &&
            playbackState != null &&
            !animationController.isFinished()
        ) {
            if (playbackState.clip.id == currentVariant.clip.id) {
                clipReuseCount += 1
                recordTransition(
                    visualState = visualState,
                    variant = currentVariant,
                    reason = REASON_REUSE_ACTIVE_VARIANT
                )
                return currentVariant
            }

            animationController.setClipIfDifferent(currentVariant.clip)
            clipActivationCount += 1
            clipResyncCount += 1
            recordTransition(
                visualState = visualState,
                variant = currentVariant,
                reason = REASON_RESYNC_ACTIVE_VARIANT
            )
            return currentVariant
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
        clipActivationCount += 1
        recordTransition(
            visualState = visualState,
            variant = selectedVariant,
            reason = if (visualStateChanged) {
                REASON_STATE_TRANSITION
            } else if (playbackState == null) {
                REASON_INITIAL_SELECTION
            } else {
                REASON_ADVANCE_VARIANT
            }
        )
        return selectedVariant
    }

    fun getActiveVisualState(): PixelPetVisualState? = activeVisualState

    fun getActiveVariant(): PixelAnimationVariant? = activeVariant

    fun getDiagnostics(): PixelAnimationOrchestratorDiagnostics {
        return PixelAnimationOrchestratorDiagnostics(
            synchronizationCount = synchronizationCount,
            stateTransitionCount = stateTransitionCount,
            clipActivationCount = clipActivationCount,
            clipReuseCount = clipReuseCount,
            clipResyncCount = clipResyncCount,
            currentVisualStateId = activeVisualState?.id,
            currentVariantId = activeVariant?.id,
            currentClipId = activeVariant?.clip?.id,
            recentTransitions = recentTransitions.toList()
        )
    }

    private fun recordTransition(
        visualState: PixelPetVisualState,
        variant: PixelAnimationVariant,
        reason: String
    ) {
        recentTransitions += PixelAnimationTransitionRecord(
            visualStateId = visualState.id,
            variantId = variant.id,
            clipId = variant.clip.id,
            reason = reason
        )
        if (recentTransitions.size > MAX_HISTORY_SIZE) {
            recentTransitions.removeAt(0)
        }
    }

    private companion object {
        private const val MAX_HISTORY_SIZE = 8
        private const val REASON_INITIAL_SELECTION = "initial_selection"
        private const val REASON_STATE_TRANSITION = "state_transition"
        private const val REASON_ADVANCE_VARIANT = "advance_variant"
        private const val REASON_REUSE_ACTIVE_VARIANT = "reuse_active_variant"
        private const val REASON_RESYNC_ACTIVE_VARIANT = "resync_active_variant"
    }
}

data class PixelAnimationOrchestratorDiagnostics(
    val synchronizationCount: Int,
    val stateTransitionCount: Int,
    val clipActivationCount: Int,
    val clipReuseCount: Int,
    val clipResyncCount: Int,
    val currentVisualStateId: String?,
    val currentVariantId: String?,
    val currentClipId: String?,
    val recentTransitions: List<PixelAnimationTransitionRecord>
)

data class PixelAnimationTransitionRecord(
    val visualStateId: String,
    val variantId: String,
    val clipId: String,
    val reason: String
)
