package com.aipet.brain.app.audio

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.audio.AudioIntent
import com.aipet.brain.brain.events.audio.LocalAudioIntentEvent
import com.aipet.brain.brain.state.BrainState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LocalAudioIntentCommandRuleTest {
    @Test
    fun `routes wake up and play random when safe`() = runTest {
        val calls = mutableListOf<String>()

        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.WAKE_UP, "wake up")
                )
            ),
            currentBrainState = { BrainState.SLEEPY },
            onWakeUp = { calls += "wake" },
            onPlayRandom = { calls += "play_random" }
        ).observeEventsAndRoute()

        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            onWakeUp = { calls += "wake" },
            onPlayRandom = { calls += "play_random" }
        ).observeEventsAndRoute()

        assertEquals(listOf("wake", "play_random"), calls)
    }

    @Test
    fun `rejects non wake commands while sleepy`() = runTest {
        val calls = mutableListOf<String>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.LEARN_PERSON, "learn person"),
                    intentEvent(AudioIntent.LEARN_OBJECT, "learn object"),
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random")
                )
            ),
            currentBrainState = { BrainState.SLEEPY },
            onLearnPerson = { calls += "learn_person" },
            onLearnObject = { calls += "learn_object" },
            onPlayRandom = { calls += "play_random" }
        ).observeEventsAndRoute()

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `rejects conflicting teach flows and play during exclusive flow`() = runTest {
        val calls = mutableListOf<String>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.LEARN_PERSON, "learn person"),
                    intentEvent(AudioIntent.LEARN_OBJECT, "learn object"),
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            isTeachPersonFlowActive = { true },
            isTeachObjectFlowActive = { false },
            isExclusiveFlowActive = { true },
            onLearnPerson = { calls += "learn_person" },
            onLearnObject = { calls += "learn_object" },
            onPlayRandom = { calls += "play_random" }
        ).observeEventsAndRoute()

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `wake up while already awake is soft no op`() = runTest {
        val calls = mutableListOf<String>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.WAKE_UP, "wake up")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            onWakeUp = { calls += "wake" }
        ).observeEventsAndRoute()

        assertEquals(emptyList<String>(), calls)
    }

    @Test
    fun `audio feedback triggers only for accepted intents`() = runTest {
        val audioFeedbackCalls = mutableListOf<AudioIntent>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random"),
                    intentEvent(AudioIntent.WAKE_UP, "wake up")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            isExclusiveFlowActive = { true },
            onPlayRandom = {},
            onAcceptedAudioFeedback = { intent ->
                audioFeedbackCalls += intent
                true
            }
        ).observeEventsAndRoute()

        assertEquals(emptyList<AudioIntent>(), audioFeedbackCalls)
    }

    @Test
    fun `accepted play random triggers audio feedback`() = runTest {
        val audioFeedbackCalls = mutableListOf<AudioIntent>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            onPlayRandom = {},
            onAcceptedAudioFeedback = { intent ->
                audioFeedbackCalls += intent
                true
            }
        ).observeEventsAndRoute()

        assertEquals(listOf(AudioIntent.PLAY_RANDOM), audioFeedbackCalls)
    }

    @Test
    fun `accepted learn person triggers avatar reaction`() = runTest {
        val avatarReactionCalls = mutableListOf<AudioIntent>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.LEARN_PERSON, "learn person")
                )
            ),
            currentBrainState = { BrainState.CURIOUS },
            onLearnPerson = {},
            onAcceptedAvatarReaction = { intent ->
                avatarReactionCalls += intent
                true
            }
        ).observeEventsAndRoute()

        assertEquals(listOf(AudioIntent.LEARN_PERSON), avatarReactionCalls)
    }

    @Test
    fun `rejected commands do not trigger avatar reaction`() = runTest {
        val avatarReactionCalls = mutableListOf<AudioIntent>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.PLAY_RANDOM, "play random")
                )
            ),
            currentBrainState = { BrainState.SLEEPY },
            onAcceptedAvatarReaction = { intent ->
                avatarReactionCalls += intent
                true
            }
        ).observeEventsAndRoute()

        assertEquals(emptyList<AudioIntent>(), avatarReactionCalls)
    }

    @Test
    fun `unknown intent is silently ignored and does not trigger any action`() = runTest {
        val actionCalls = mutableListOf<String>()
        buildRule(
            eventBus = StaticEventBus(
                events = listOf(
                    intentEvent(AudioIntent.UNKNOWN, "[unk]")
                )
            ),
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
        ).observeEventsAndRoute()

        assertEquals(emptyList<String>(), actionCalls)
    }

    private fun buildRule(
        eventBus: EventBus,
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

    private fun intentEvent(
        intent: AudioIntent,
        rawText: String
    ): EventEnvelope {
        return EventEnvelope.create(
            type = EventType.LOCAL_AUDIO_INTENT_DETECTED,
            payloadJson = LocalAudioIntentEvent(
                intent = intent,
                confidence = 0.4f,
                rawText = rawText
            ).toJson()
        )
    }

    private class StaticEventBus(
        private val events: List<EventEnvelope>
    ) : EventBus {
        override suspend fun publish(event: EventEnvelope) = Unit

        override fun observe(): Flow<EventEnvelope> {
            return events.asFlow()
        }
    }
}
