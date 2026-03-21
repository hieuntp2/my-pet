package com.aipet.brain.pixel.avatar.controller

import androidx.compose.ui.graphics.ImageBitmap
import com.aipet.brain.pixel.avatar.model.AnimationStateSet
import com.aipet.brain.pixel.avatar.model.AnimationVariant
import com.aipet.brain.pixel.avatar.model.PixelFrame64
import com.aipet.brain.pixel.avatar.model.PetVisualState
import com.aipet.brain.pixel.avatar.renderer.toImageBitmap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.random.Random

/**
 * Drives the frame-by-frame advancement of pixel pet animations.
 *
 * Responsibilities:
 * - Accepts a target [PetVisualState] and selects a weighted variant from the registry.
 * - Advances through frames according to each frame's holdMs.
 * - Exposes the current frame as a [StateFlow<PixelFrame64>] for the composable layer.
 * - Exposes pre-converted [ImageBitmap] cache via [currentBitmap] to avoid per-frame allocation.
 * - Supports debug overrides for forcing state, variant, and pause.
 *
 * The UI layer collects [currentFrame] (or [currentBitmap]) and renders it.
 * All timing and selection policy lives here, not in composables.
 */
class PixelAnimationController(
    private val scope: CoroutineScope,
    private val stateRegistry: Map<PetVisualState, AnimationStateSet>,
    private val random: Random = Random.Default
) {
    private val _currentFrame = MutableStateFlow(placeholderFrame())
    val currentFrame: StateFlow<PixelFrame64> = _currentFrame.asStateFlow()

    // Cached bitmaps for each frame in the active clip to avoid per-frame Bitmap allocation.
    private var clipBitmapCache: List<ImageBitmap> = emptyList()

    private val _currentBitmapIndex = MutableStateFlow(0)
    val currentBitmapIndex: StateFlow<Int> = _currentBitmapIndex.asStateFlow()

    private val _debugState = MutableStateFlow(DebugAnimationOverride())
    val debugState: StateFlow<DebugAnimationOverride> = _debugState.asStateFlow()

    // Metadata exposed for preview screen
    private val _activeStateName = MutableStateFlow(PetVisualState.NEUTRAL.name)
    val activeStateName: StateFlow<String> = _activeStateName.asStateFlow()

    private val _activeVariantName = MutableStateFlow("")
    val activeVariantName: StateFlow<String> = _activeVariantName.asStateFlow()

    private val _activeFrameIndex = MutableStateFlow(0)
    val activeFrameIndex: StateFlow<Int> = _activeFrameIndex.asStateFlow()

    private val _activeFrameTotal = MutableStateFlow(1)
    val activeFrameTotal: StateFlow<Int> = _activeFrameTotal.asStateFlow()

    private val _activeHoldMs = MutableStateFlow(0L)
    val activeHoldMs: StateFlow<Long> = _activeHoldMs.asStateFlow()

    private var requestedState: PetVisualState = PetVisualState.NEUTRAL
    private var playbackJob: Job? = null

    init {
        startPlayback(PetVisualState.NEUTRAL)
    }

    /**
     * Change the active visual state. If the new state differs from the current one,
     * the controller immediately transitions and selects a new variant.
     */
    fun setVisualState(state: PetVisualState) {
        requestedState = state
        restartPlayback(state)
    }

    /**
     * Debug: force a specific variant index for a given state (0-based).
     * Use null to clear the override.
     */
    fun forceVariant(state: PetVisualState, variantIndex: Int?) {
        _debugState.value = _debugState.value.copy(
            forcedState = state,
            forcedVariantIndex = variantIndex
        )
        restartPlayback(state)
    }

    /** Debug: pause frame advancement. */
    fun pause() {
        _debugState.value = _debugState.value.copy(paused = true)
    }

    /** Debug: resume frame advancement from current position. */
    fun resume() {
        _debugState.value = _debugState.value.copy(paused = false)
    }

    /** Debug: restart the current clip from frame 0. */
    fun restartClip() {
        restartPlayback(requestedState)
    }

    /** Debug: clear all overrides and return to normal operation. */
    fun clearDebugOverrides() {
        _debugState.value = DebugAnimationOverride()
        restartPlayback(requestedState)
    }

    // ---------- internals ----------

    private fun startPlayback(state: PetVisualState) {
        playbackJob?.cancel()
        playbackJob = scope.launch {
            while (isActive) {
                val override = _debugState.value
                val effectiveState = override.forcedState ?: state
                val stateSet = stateRegistry[effectiveState] ?: stateRegistry[PetVisualState.NEUTRAL] ?: return@launch
                val variant = selectVariant(stateSet, override.forcedVariantIndex)
                val clip = variant.clip

                // Cache bitmaps for entire clip upfront (avoids per-frame Bitmap creation)
                clipBitmapCache = clip.frames.map { it.frame.toImageBitmap() }

                _activeStateName.value = effectiveState.name
                _activeVariantName.value = variant.name
                _activeFrameTotal.value = clip.frames.size

                var frameIndex = 0
                while (isActive) {
                    if (!_debugState.value.paused) {
                        val entry = clip.frames[frameIndex]
                        _currentFrame.value = entry.frame
                        _activeFrameIndex.value = frameIndex
                        _activeHoldMs.value = entry.holdMs
                        _currentBitmapIndex.value = frameIndex

                        delay(entry.holdMs)

                        frameIndex++
                        if (frameIndex >= clip.frames.size) {
                            if (clip.loop) {
                                frameIndex = 0
                            } else {
                                break // one-shot: exit inner loop, re-select variant
                            }
                        }
                    } else {
                        delay(50) // poll while paused
                    }
                    // Re-check if a new state was requested (from setVisualState during playback)
                    val latestOverride = _debugState.value
                    val latestEffective = latestOverride.forcedState ?: requestedState
                    if (latestEffective != effectiveState) break
                }
            }
        }
    }

    private fun restartPlayback(state: PetVisualState) {
        startPlayback(state)
    }

    private fun selectVariant(
        stateSet: AnimationStateSet,
        forcedIndex: Int?
    ): AnimationVariant {
        if (forcedIndex != null) {
            val clamped = forcedIndex.coerceIn(0, stateSet.variants.lastIndex)
            return stateSet.variants[clamped]
        }
        val totalWeight = stateSet.variants.fold(0f) { acc, v -> acc + v.weight }
        var roll = random.nextFloat() * totalWeight
        for (variant in stateSet.variants) {
            roll -= variant.weight
            if (roll <= 0f) return variant
        }
        return stateSet.variants.last()
    }

    private fun placeholderFrame(): PixelFrame64 {
        val blankRow = ".".repeat(64)
        return PixelFrame64(List(64) { blankRow })
    }
}
