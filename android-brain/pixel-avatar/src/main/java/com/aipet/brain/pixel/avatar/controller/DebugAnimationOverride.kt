package com.aipet.brain.pixel.avatar.controller

import com.aipet.brain.pixel.avatar.model.PetVisualState

/**
 * Debug override that forces the animation controller to a specific state and/or variant.
 * Only used in preview/debug contexts. Has no effect in production mode.
 *
 * @param forcedState    If set, controller ignores incoming state requests and plays this state.
 * @param forcedVariantIndex If set, controller ignores weighted selection and plays this variant index.
 * @param paused         If true, frame advancement is suspended.
 */
data class DebugAnimationOverride(
    val forcedState: PetVisualState? = null,
    val forcedVariantIndex: Int? = null,
    val paused: Boolean = false
)
