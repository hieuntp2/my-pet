package com.aipet.brain.app.avatar

import com.aipet.brain.brain.attention.AttentionMode
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

data class HomePixelPetAvatarBridgeInput(
    val hasAudioAttention: Boolean,
    val hasDirectEngagement: Boolean,
    val hasExcitedEmotion: Boolean,
    val hasAttentiveInterest: Boolean,
    val hasHungerNeed: Boolean,
    val hasLowEnergy: Boolean,
    val hasLonelyNeed: Boolean,
    val hasSadEmotion: Boolean,
    val hasPerceptionLooking: Boolean,
    val hasPerceptionAsking: Boolean,
    // Non-null when behavior-driven execution is actively controlling home intent.
    val behaviorDrivenIntent: PixelPetAvatarIntent? = null,
    // Source intention and live attention snapshot used to tune behavior-driven intent.
    val behaviorSourceIntention: PetIntention? = null,
    val behaviorAttentionMode: AttentionMode? = null,
    val behaviorAttentionIntensity: Float = 0f,
    // Non-null during the greeting window; highest priority override in the intent resolver.
    val greetingBoostIntent: PixelPetAvatarIntent? = null,
    // Non-null for the reaction window after a tap/long-press; second-highest priority.
    val transientReactionIntent: PixelPetAvatarIntent? = null,
    // Non-null for a short window after a sound stimulus; third priority.
    val soundReactionIntent: PixelPetAvatarIntent? = null,
    val sourceSummary: String
)
