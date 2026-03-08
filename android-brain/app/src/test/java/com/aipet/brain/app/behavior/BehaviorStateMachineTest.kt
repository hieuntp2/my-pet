package com.aipet.brain.app.behavior

import com.aipet.brain.app.reactions.OwnerSeenReactionEngine
import com.aipet.brain.app.reactions.PersonSeenSource
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BehaviorStateMachineTest {
    @Test
    fun defaultState_isIdle() {
        val machine = BehaviorStateMachine(
            nowProvider = { 1_000L }
        )

        val current = machine.getCurrentBehaviorState()

        assertEquals(RobotBehaviorState.IDLE, current.currentState)
        assertEquals(1_000L, current.updatedAtMs)
        assertNull(current.lastTransition)
    }

    @Test
    fun ownerSeenFlow_transitionsToGreetingOwner() = runTest {
        val eventBus = FakeEventBus()
        val reactionEngine = OwnerSeenReactionEngine(eventBus = eventBus)
        val machine = BehaviorStateMachine()

        val reactionJob = launch {
            reactionEngine.observePersonSeenUpdates()
        }
        val behaviorJob = launch {
            machine.observeEventsAndApplyTransitions(eventBus)
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 2_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-owner",
                    seenAtMs = 2_000L,
                    seenCount = 3,
                    isOwner = true,
                    source = PersonSeenSource.DIRECT_PERSON_DEBUG_ACTION.name
                ).toJson()
            )
        )
        advanceUntilIdle()

        val current = machine.getCurrentBehaviorState()
        assertEquals(RobotBehaviorState.GREETING_OWNER, current.currentState)
        assertEquals(
            BehaviorTransitionReason.OWNER_GREETING_TRIGGERED,
            current.lastTransition?.reason
        )
        assertEquals(
            listOf(
                EventType.PERSON_SEEN_RECORDED,
                EventType.OWNER_SEEN_DETECTED,
                EventType.ROBOT_GREETING_OWNER_TRIGGERED
            ),
            eventBus.publishedEvents.map { event -> event.type }
        )

        reactionJob.cancel()
        behaviorJob.cancel()
    }

    @Test
    fun nonOwnerSeen_doesNotEnterGreetingOwnerState() = runTest {
        val eventBus = FakeEventBus()
        val reactionEngine = OwnerSeenReactionEngine(eventBus = eventBus)
        val machine = BehaviorStateMachine()

        val reactionJob = launch {
            reactionEngine.observePersonSeenUpdates()
        }
        val behaviorJob = launch {
            machine.observeEventsAndApplyTransitions(eventBus)
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 3_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-guest",
                    seenAtMs = 3_000L,
                    seenCount = 1,
                    isOwner = false,
                    source = PersonSeenSource.LINKED_PROFILE_OBSERVATION_BRIDGE.name
                ).toJson()
            )
        )
        advanceUntilIdle()

        val current = machine.getCurrentBehaviorState()
        assertEquals(RobotBehaviorState.ATTENTIVE, current.currentState)
        assertEquals(
            BehaviorTransitionReason.NON_OWNER_SEEN,
            current.lastTransition?.reason
        )
        assertFalse(eventBus.publishedEvents.any { event ->
            event.type == EventType.ROBOT_GREETING_OWNER_TRIGGERED
        })

        reactionJob.cancel()
        behaviorJob.cancel()
    }
}

private class FakeEventBus : EventBus {
    private val eventsFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val publishedEvents = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
        eventsFlow.emit(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return eventsFlow.asSharedFlow()
    }
}
