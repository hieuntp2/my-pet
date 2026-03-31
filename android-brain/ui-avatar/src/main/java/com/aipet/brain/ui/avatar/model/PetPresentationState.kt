package com.aipet.brain.ui.avatar.model

import com.aipet.brain.ui.avatar.pixel.model.PixelPetVisualState
import com.aipet.brain.ui.avatar.pixel.model.Neutral

/**
 * Canonical presentation state for the pet avatar at any given moment.
 *
 * This is the single contract used across greeting, tap, activity, idle, and sound reactions.
 * It separates the concerns of:
 *   - what emotion is dominant (dominantVisualState)
 *   - what animation purpose is active (animationIntent)
 *   - optional micro-state adjustment (emotionModifier)
 *   - which specific authored variant should play (animationVariantId — null = let selector decide)
 *   - what the pet should say/show in a bubble (bubblePayload — null = no bubble)
 */
data class PetPresentationState(
    val dominantVisualState: PixelPetVisualState = Neutral,
    val animationIntent: AnimationIntent = AnimationIntent.IDLE_RESTING,
    val emotionModifier: PetEmotionModifier? = null,
    val animationVariantId: String? = null,
    val bubblePayload: PetBubblePayload? = null
)
