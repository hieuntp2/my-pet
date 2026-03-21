package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.VoiceActivityStimulus
import com.aipet.brain.brain.logic.audio.VoiceActivityStimulusState
import com.aipet.brain.brain.logic.audio.toDebugSummary
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeDebugMetadata
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState

class RealPixelPetBridgeStateAdapter(
    private val intentResolver: HomePixelPetAvatarIntentResolver = HomePixelPetAvatarIntentResolver()
) {
    private var previousIntentResolution: HomePixelPetAvatarIntentResolution? = null

    fun map(signal: HomePixelPetAvatarSignal): PixelPetBridgeState {
        val bridgeInput = buildBridgeInput(signal)
        val resolution = intentResolver.resolve(
            bridgeInput = bridgeInput,
            previousResolution = previousIntentResolution
        )
        previousIntentResolution = resolution
        return PixelPetBridgeState(
            intent = resolution.intent,
            debugMetadata = PixelPetBridgeDebugMetadata(
                chosenIntent = resolution.intent.name.lowercase(),
                priorityReason = resolution.decisionReason,
                sourceSummary = resolution.sourceSummary,
                policySummary = resolution.policySummary
            )
        )
    }

    fun getLatestIntentResolution(): HomePixelPetAvatarIntentResolution? = previousIntentResolution

    private fun buildBridgeInput(signal: HomePixelPetAvatarSignal): HomePixelPetAvatarBridgeInput {
        val hasAudioAttention = signal.latestAudioStimulus is KeywordStimulus ||
            (signal.latestAudioStimulus is VoiceActivityStimulus &&
                signal.latestAudioStimulus.state == VoiceActivityStimulusState.STARTED)
        val hasDirectEngagement = signal.brainState == BrainState.HAPPY ||
            signal.petEmotion == PetEmotion.HAPPY ||
            signal.petEmotion == PetEmotion.EXCITED
        val hasAttentiveInterest = signal.brainState == BrainState.CURIOUS ||
            signal.petEmotion == PetEmotion.CURIOUS ||
            signal.petEmotion == PetEmotion.HUNGRY
        val hasLowEnergy = signal.conditions.contains(PetCondition.SLEEPY) ||
            signal.petEmotion == PetEmotion.SLEEPY ||
            signal.brainState == BrainState.SLEEPY

        val sourceSummary = buildList {
            if (hasAudioAttention) add("audio_attention")
            if (hasDirectEngagement) add("direct_engagement")
            if (hasAttentiveInterest) add("attentive_interest")
            if (hasLowEnergy) add("low_energy")
            signal.latestAudioStimulus?.let { add(it.toDebugSummary()) }
        }.ifEmpty {
            listOf("neutral_fallback")
        }.joinToString(separator = " | ")

        return HomePixelPetAvatarBridgeInput(
            hasAudioAttention = hasAudioAttention,
            hasDirectEngagement = hasDirectEngagement,
            hasAttentiveInterest = hasAttentiveInterest,
            hasLowEnergy = hasLowEnergy,
            sourceSummary = sourceSummary
        )
    }
}
