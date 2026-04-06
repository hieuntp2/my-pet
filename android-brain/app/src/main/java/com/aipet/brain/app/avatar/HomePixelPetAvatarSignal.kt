package com.aipet.brain.app.avatar

import com.aipet.brain.brain.attention.AttentionMode
import com.aipet.brain.brain.b2.domain.PetIntention
import com.aipet.brain.brain.logic.audio.AudioStimulus
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

data class HomePixelPetAvatarSignal(
    val petEmotion: PetEmotion,
    val conditions: Set<PetCondition>,
    val brainState: BrainState,
    val latestAudioStimulus: AudioStimulus? = null,
    val isPerceptionLooking: Boolean = false,
    val isPerceptionAsking: Boolean = false,
    // Non-null when behavior-experience execution owns the current home reaction.
    val behaviorDrivenIntent: PixelPetAvatarIntent? = null,
    // Source intention currently executed by behavior-experience layer.
    val behaviorSourceIntention: PetIntention? = null,
    // Live attention mode/intensity injected into behavior-driven intent tuning.
    val behaviorAttentionMode: AttentionMode? = null,
    val behaviorAttentionIntensity: Float = 0f,
    // Non-null during the app-open greeting window; drives the highest-priority avatar intent.
    val greetingBoostIntent: PixelPetAvatarIntent? = null,
    // Non-null for [TapReactionPresentationMapper.REACTION_DURATION_MS] after a tap/long-press.
    val transientReactionIntent: PixelPetAvatarIntent? = null,
    // Non-null for a short window after a sound stimulus; third-priority override.
    val soundReactionIntent: PixelPetAvatarIntent? = null
)
