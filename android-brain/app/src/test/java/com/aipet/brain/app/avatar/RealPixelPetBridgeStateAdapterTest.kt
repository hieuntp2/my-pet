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
import com.aipet.brain.ui.avatar.pixel.model.Asking
import com.aipet.brain.ui.avatar.pixel.model.Curious
import com.aipet.brain.ui.avatar.pixel.model.Excited
import com.aipet.brain.ui.avatar.pixel.model.Happy
import com.aipet.brain.ui.avatar.pixel.model.Hungry
import com.aipet.brain.ui.avatar.pixel.model.Looking
import com.aipet.brain.ui.avatar.pixel.model.Sad
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
    fun `perception signals resolve looking and asking intents`() {
        val adapter = RealPixelPetBridgeStateAdapter()
        val mapper = DefaultPixelPetStateMapper()

        val looking = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.IDLE,
                conditions = emptySet(),
                brainState = BrainState.IDLE,
                isPerceptionLooking = true
            )
        )
        val asking = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.IDLE,
                conditions = emptySet(),
                brainState = BrainState.IDLE,
                isPerceptionAsking = true
            )
        )

        assertEquals(PixelPetAvatarIntent.LOOKING, looking.intent)
        assertEquals(PixelPetAvatarIntent.ASKING, asking.intent)
        assertEquals(Looking, mapper.map(looking))
        assertEquals(Asking, mapper.map(asking))
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
        assertTrue(resolution?.policySummary?.contains("processing>engaged>excited>asking>looking") == true)
    }

    @Test
    fun `bridge state debug metadata can be rendered as log summary`() {
        val metadata = PixelPetBridgeState(
            intent = PixelPetAvatarIntent.PROCESSING,
            debugMetadata = com.aipet.brain.ui.avatar.pixel.bridge.PixelPetBridgeDebugMetadata(
                chosenIntent = "processing",
                priorityReason = "audio_attention_detected",
                sourceSummary = "audio_attention",
                policySummary = "processing>engaged>excited>asking>looking>hungry>low_energy>sad>attentive>neutral; keep_previous_over_neutral=true"
            )
        ).debugMetadata

        assertEquals(
            "intent=processing reason=audio_attention_detected policy=processing>engaged>excited>asking>looking>hungry>low_energy>sad>attentive>neutral; keep_previous_over_neutral=true sources=audio_attention",
            metadata?.toLogSummary()
        )
    }

    @Test
    fun `sad emotion resolves sad intent and visual state`() {
        val adapter = RealPixelPetBridgeStateAdapter()
        val mapper = DefaultPixelPetStateMapper()

        val sadState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.SAD,
                conditions = emptySet(),
                brainState = BrainState.IDLE
            )
        )

        assertEquals(PixelPetAvatarIntent.SAD, sadState.intent)
        assertEquals(Sad, mapper.map(sadState))
    }

    @Test
    fun `excited emotion resolves excited intent and visual state`() {
        val adapter = RealPixelPetBridgeStateAdapter()
        val mapper = DefaultPixelPetStateMapper()

        val excitedState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.EXCITED,
                conditions = emptySet(),
                brainState = BrainState.IDLE
            )
        )

        assertEquals(PixelPetAvatarIntent.EXCITED, excitedState.intent)
        assertEquals(Excited, mapper.map(excitedState))
    }

    @Test
    fun `hungry emotion resolves hungry intent and visual state`() {
        val adapter = RealPixelPetBridgeStateAdapter()
        val mapper = DefaultPixelPetStateMapper()

        val hungryState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.HUNGRY,
                conditions = emptySet(),
                brainState = BrainState.IDLE
            )
        )

        assertEquals(PixelPetAvatarIntent.HUNGRY, hungryState.intent)
        assertEquals(Hungry, mapper.map(hungryState))
    }

    @Test
    fun `greeting boost intent overrides all other signals including audio attention`() {
        val adapter = RealPixelPetBridgeStateAdapter()

        val greetingState = adapter.map(
            HomePixelPetAvatarSignal(
                petEmotion = PetEmotion.HAPPY,
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
                ),
                greetingBoostIntent = com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent.ENGAGED
            )
        )

        // Greeting boost wins over audio_attention (PROCESSING) because greeting is highest priority
        assertEquals(PixelPetAvatarIntent.ENGAGED, greetingState.intent)
        assertEquals("greeting_active_boost", greetingState.debugMetadata?.priorityReason)
        assertTrue(greetingState.debugMetadata?.sourceSummary?.contains("greeting_active") == true)
    }
}
