package com.aipet.brain.app.avatar

import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

data class HomePixelPetAvatarBridgeInput(
    val hasAudioAttention: Boolean,
    val hasDirectEngagement: Boolean,
    val hasExcitedEmotion: Boolean,
    val hasAttentiveInterest: Boolean,
    val hasLowEnergy: Boolean,
    val hasSadEmotion: Boolean,
    val hasHungryEmotion: Boolean,
    val hasPerceptionLooking: Boolean,
    val hasPerceptionAsking: Boolean,
    // Non-null during the greeting window — highest priority override in the intent resolver.
    val greetingBoostIntent: PixelPetAvatarIntent? = null,
    // Non-null for the reaction window after a tap/long-press — second-highest priority.
    val transientReactionIntent: PixelPetAvatarIntent? = null,
    // Non-null for a short window after a sound stimulus — third priority.
    val soundReactionIntent: PixelPetAvatarIntent? = null,
    val sourceSummary: String
)
