package com.aipet.brain.app.avatar

import com.aipet.brain.brain.logic.audio.AudioStimulus
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.state.BrainState

data class HomePixelPetAvatarSignal(
    val petEmotion: PetEmotion,
    val conditions: Set<PetCondition>,
    val brainState: BrainState,
    val latestAudioStimulus: AudioStimulus? = null
)
