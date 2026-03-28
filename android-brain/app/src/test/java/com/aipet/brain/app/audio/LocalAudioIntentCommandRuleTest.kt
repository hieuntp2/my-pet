package com.aipet.brain.app.audio

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.InMemoryEventBus
import com.aipet.brain.brain.events.audio.AudioIntent
import com.aipet.brain.brain.events.audio.LocalAudioIntentEvent
import com.aipet.brain.brain.state.BrainState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAudioIntentCommandRuleTest {
    @Test
    fun `routes wake up and play random when safe`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val calls = mutableListOf<String>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.SLEEPY },
            onWakeUp = { calls += "wake" },
            onPlayRandom = { calls += "play_random" }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.WAKE_UP, "wake up")
        advanceUntilIdle()

        // switch to awake for play command
        val awakeRule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            onWakeUp = { calls += "wake" },
            onPlayRandom = { calls += "play_random" }
        )
        job.cancel()
        val job2 = backgroundScope.launch { awakeRule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        advanceUntilIdle()

        job2.cancel()
        assertEquals(listOf("wake", "play_random"), calls)
    }

    @Test
    fun `rejects non wake commands while sleepy`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val calls = mutableListOf<String>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.SLEEPY },
            onLearnPerson = { calls += "learn_person" },
            onLearnObject = { calls += "learn_object" },
            onPlayRandom = { calls += "play_random" }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.LEARN_PERSON, "learn person")
        emitIntent(eventBus, AudioIntent.LEARN_OBJECT, "learn object")
        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `rejects conflicting teach flows and play during exclusive flow`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val calls = mutableListOf<String>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            isTeachPersonFlowActive = { true },
            isTeachObjectFlowActive = { false },
            isExclusiveFlowActive = { true },
            onLearnPerson = { calls += "learn_person" },
            onLearnObject = { calls += "learn_object" },
            onPlayRandom = { calls += "play_random" }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.LEARN_PERSON, "learn person")
        emitIntent(eventBus, AudioIntent.LEARN_OBJECT, "learn object")
        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `wake up while already awake is soft no op`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val calls = mutableListOf<String>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            onWakeUp = { calls += "wake" }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.WAKE_UP, "wake up")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `audio feedback triggers only for accepted intents`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val audioFeedbackCalls = mutableListOf<AudioIntent>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            isExclusiveFlowActive = { true },
            onPlayRandom = {},
            onAcceptedAudioFeedback = { intent ->
                audioFeedbackCalls += intent
                true
            }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        emitIntent(eventBus, AudioIntent.WAKE_UP, "wake up")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<AudioIntent>(), audioFeedbackCalls)
    }

    @Test
    fun `accepted play random triggers audio feedback`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val audioFeedbackCalls = mutableListOf<AudioIntent>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            onPlayRandom = {},
            onAcceptedAudioFeedback = { intent ->
                audioFeedbackCalls += intent
                true
            }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        advanceUntilIdle()

        job.cancel()
        assertEquals(listOf(AudioIntent.PLAY_RANDOM), audioFeedbackCalls)
    }

    @Test
    fun `accepted learn person triggers avatar reaction`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val avatarReactionCalls = mutableListOf<AudioIntent>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            onLearnPerson = {},
            onAcceptedAvatarReaction = { intent ->
                avatarReactionCalls += intent
                true
            }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.LEARN_PERSON, "learn person")
        advanceUntilIdle()

        job.cancel()
        assertEquals(listOf(AudioIntent.LEARN_PERSON), avatarReactionCalls)
    }

    @Test
    fun `rejected commands do not trigger avatar reaction`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val avatarReactionCalls = mutableListOf<AudioIntent>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.SLEEPY },
            onAcceptedAvatarReaction = { intent ->
                avatarReactionCalls += intent
                true
            }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.PLAY_RANDOM, "play random")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<AudioIntent>(), avatarReactionCalls)
    }

    @Test
    fun `unknown intent is silently ignored and does not trigger any action`() = runTest {
        val eventBus = InMemoryEventBus(replay = 0)
        val actionCalls = mutableListOf<String>()
        val rule = buildRule(
            eventBus = eventBus,
            currentBrainState = { BrainState.CURIOUS },
            onWakeUp = { actionCalls += "wake" },
            onLearnPerson = { actionCalls += "learn_person" },
            onLearnObject = { actionCalls += "learn_object" },
            onPlayRandom = { actionCalls += "play_random" },
            onAcceptedAudioFeedback = { intent ->
                actionCalls += "audio_feedback_${intent.name}"
                true
            },
            onAcceptedAvatarReaction = { intent ->
                actionCalls += "avatar_reaction_${intent.name}"
                true
            }
        )
        val job = backgroundScope.launch { rule.observeEventsAndRoute() }

        emitIntent(eventBus, AudioIntent.UNKNOWN, "[unk]")
        advanceUntilIdle()

        job.cancel()
        assertEquals(emptyList<String>(), actionCalls)
    }

    private fun buildRule(
        eventBus: InMemoryEventBus,
        currentBrainState: () -> BrainState,
        isTeachPersonFlowActive: () -> Boolean = { false },
        isTeachObjectFlowActive: () -> Boolean = { false },
        isExclusiveFlowActive: () -> Boolean = { false },
        isPlayRandomEntryAvailable: () -> Boolean = { true },
        onAcceptedAudioFeedback: suspend (AudioIntent) -> Boolean = { false },
        onAcceptedAvatarReaction: suspend (AudioIntent) -> Boolean = { false },
        onWakeUp: suspend (Long) -> Unit = {},
        onLearnPerson: suspend () -> Unit = {},
        onLearnObject: suspend () -> Unit = {},
        onPlayRandom: suspend () -> Unit = {}
    ): LocalAudioIntentCommandRule {
        return LocalAudioIntentCommandRule(
            eventBus = eventBus,
            currentBrainState = currentBrainState,
            isTeachPersonFlowActive = isTeachPersonFlowActive,
            isTeachObjectFlowActive = isTeachObjectFlowActive,
            isExclusiveFlowActive = isExclusiveFlowActive,
            isPlayRandomEntryAvailable = isPlayRandomEntryAvailable,
            onAcceptedAudioFeedback = onAcceptedAudioFeedback,
            onAcceptedAvatarReaction = onAcceptedAvatarReaction,
            onWakeUp = onWakeUp,
            onLearnPerson = onLearnPerson,
            onLearnObject = onLearnObject,
            onPlayRandom = onPlayRandom
        )
    }

    private suspend fun emitIntent(
        eventBus: InMemoryEventBus,
        intent: AudioIntent,
        rawText: String
    ) {
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.LOCAL_AUDIO_INTENT_DETECTED,
                payloadJson = LocalAudioIntentEvent(
                    intent = intent,
                    confidence = 0.4f,
                    rawText = rawText
                ).toJson()
            )
        )
    }
}
