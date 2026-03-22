package com.aipet.brain.app.avatar

data class HomePixelPetAvatarBridgeInput(
    val hasAudioAttention: Boolean,
    val hasDirectEngagement: Boolean,
    val hasAttentiveInterest: Boolean,
    val hasLowEnergy: Boolean,
    val hasPerceptionLooking: Boolean,
    val hasPerceptionAsking: Boolean,
    val sourceSummary: String
)
