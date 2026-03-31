package com.aipet.brain.app.avatar

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
    // Non-null during the app-open greeting window; drives the highest-priority avatar intent.
    val greetingBoostIntent: PixelPetAvatarIntent? = null,
    // Non-null for [TapReactionPresentationMapper.REACTION_DURATION_MS] after a tap/long-press.
    val transientReactionIntent: PixelPetAvatarIntent? = null,
    // Non-null for a short window after a sound stimulus; third-priority override.
    val soundReactionIntent: PixelPetAvatarIntent? = null
)
