package com.aipet.brain.ui.avatar.pixel.bridge

import com.aipet.brain.ui.avatar.pixel.model.PixelPetAnimationStateSet
import com.aipet.brain.ui.avatar.pixel.model.PixelPetVisualState

class PixelAnimationSetRegistry(
    private val animationSetsByState: Map<PixelPetVisualState, PixelPetAnimationStateSet>
) {
    init {
        require(animationSetsByState.isNotEmpty()) {
            "PixelAnimationSetRegistry must contain at least one animation set."
        }
        require(animationSetsByState.all { (state, animationSet) -> state == animationSet.state }) {
            "Registry keys must match each animation set's declared state."
        }
    }

    operator fun get(state: PixelPetVisualState): PixelPetAnimationStateSet? = animationSetsByState[state]

    fun requireAnimationSet(state: PixelPetVisualState): PixelPetAnimationStateSet {
        return requireNotNull(animationSetsByState[state]) {
            "No animation set registered for visual state '${state.id}'."
        }
    }
}
