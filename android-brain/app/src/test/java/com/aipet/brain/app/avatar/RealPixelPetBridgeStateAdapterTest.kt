package com.aipet.brain.app.avatar

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.logic.audio.KeywordStimulus
import com.aipet.brain.brain.logic.audio.KeywordStimulusKind
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetEmotion
import com.aipet.brain.brain.state.BrainState
import com.aipet.brain.ui.avatar.pixel.bridge.DefaultPixelPetStateMapper
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeState
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Thinking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RealPixelPetBridgeStateAdapterTest {

    @Test
    fun `audio attention signal resolves processing intent`() {
        val adapter = RealPixelPetBridgeStateAdapter()

        val bridgeState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.IDLE,
                conditions = emptySet(),
                brainState = BrainState.IDLE,
                latestAudioStimulus = KeywordStimulus(
                    timestampMs = 10L,
                    sourceEventType = EventType.AUDIO_RESPONSE_REQUESTED,
                    kind = KeywordStimulusKind.WAKE_WORD,
                    keywordId = "wake_word",
                    keywordText = "hey pet",
                    confidence = 0.9f,
                    engine = "test"
                )
            )
        )

        assertEquals(PixelPetAvatarIntent.PROCESSING, bridgeState.intent)
        assertEquals("processing", bridgeState.debugMetadata?.chosenIntent)
        assertTrue(bridgeState.debugMetadata?.sourceSummary?.contains("audio_attention") == true)
    }

    @Test
    fun `direct engagement overrides low energy when signals conflict`() {
        val adapter = RealPixelPetBridgeStateAdapter()

        val bridgeState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.HAPPY,
                conditions = setOf(PetCondition.SLEEPY),
                brainState = BrainState.SLEEPY
            )
        )

        assertEquals(PixelPetAvatarIntent.ENGAGED, bridgeState.intent)
        assertEquals("direct_engagement_signal", bridgeState.debugMetadata?.priorityReason)
    }

    @Test
    fun `repeated neutral fallback does not override prior strong intent`() {
        val adapter = RealPixelPetBridgeStateAdapter()

        val engagedBridgeState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.HAPPY,
                conditions = emptySet(),
                brainState = BrainState.HAPPY
            )
        )
        val heldBridgeState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.IDLE,
                conditions = emptySet(),
                brainState = BrainState.IDLE
            )
        )

        assertEquals(PixelPetAvatarIntent.ENGAGED, engagedBridgeState.intent)
        assertEquals(PixelPetAvatarIntent.ENGAGED, heldBridgeState.intent)
        assertEquals(
            "hold_previous_engaged_over_neutral",
            heldBridgeState.debugMetadata?.priorityReason
        )
    }

    @Test
    fun `downstream mapper still selects correct visual state after intent resolution`() {
        val adapter = RealPixelPetBridgeStateAdapter()
        val mapper = DefaultPixelPetStateMapper()

        val attentiveState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.CURIOUS,
                conditions = emptySet(),
                brainState = BrainState.CURIOUS
            )
        )
        val engagedState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.HAPPY,
                conditions = emptySet(),
                brainState = BrainState.HAPPY
            )
        )
        val thinkingState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.IDLE,
                conditions = emptySet(),
                brainState = BrainState.IDLE,
                latestAudioStimulus = KeywordStimulus(
                    timestampMs = 42L,
                    sourceEventType = EventType.AUDIO_RESPONSE_REQUESTED,
                    kind = KeywordStimulusKind.KEYWORD,
                    keywordId = "listen",
                    keywordText = "listen",
                    confidence = 0.8f,
                    engine = "test"
                )
            )
        )

        assertEquals(Curious, mapper.map(attentiveState))
        assertEquals(Happy, mapper.map(engagedState))
        assertEquals(Thinking, mapper.map(thinkingState))
        assertNotNull(thinkingState.debugMetadata)
    }

    @Test
    fun `adapter exposes latest intent resolution for debug inspection`() {
        val adapter = RealPixelPetBridgeStateAdapter()

        adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.CURIOUS,
                conditions = emptySet(),
                brainState = BrainState.CURIOUS
            )
        )

        val resolution = adapter.getLatestIntentResolution()

        assertNotNull(resolution)
        assertEquals(PixelPetAvatarIntent.ATTENTIVE, resolution?.intent)
        assertTrue(resolution?.policySummary?.contains("processing>engaged") == true)
    }

    @Test
    fun `bridge state debug metadata can be rendered as log summary`() {
        val metadata = PixelPetBridgeState(
            intent = PixelPetAvatarIntent.PROCESSING,
            debugMetadata = com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeDebugMetadata(
                chosenIntent = "processing",
                priorityReason = "audio_attention_detected",
                sourceSummary = "audio_attention",
                policySummary = "processing>engaged>low_energy>attentive>neutral"
            )
        ).debugMetadata

        assertEquals(
            "intent=processing reason=audio_attention_detected policy=processing>engaged>low_energy>attentive>neutral sources=audio_attention",
            metadata?.toLogSummary()
        )
    }
}
