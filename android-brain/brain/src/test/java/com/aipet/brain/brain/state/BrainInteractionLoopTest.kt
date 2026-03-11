package com.aipet.brain.brain.state

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class BrainInteractionLoopTest {
    @Test
    fun personRecognized_transitionsToHappy() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.IDLE, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )
        val job = launch { loop.observeEventsAndApplyTransitions() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 2_000L,
                payloadJson = "{}"
            )
        )
        advanceUntilIdle()

        assertEquals(BrainState.HAPPY, store.currentSnapshot().currentState)
        assertTrue(eventBus.publishedEvents.any { it.type == EventType.PERSON_RECOGNIZED })

        job.cancel()
    }

    @Test
    fun unknownPerson_transitionsToCurious() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.IDLE, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )
        val job = launch { loop.observeEventsAndApplyTransitions() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN,
                timestampMs = 2_500L,
                payloadJson = "{}"
            )
        )
        advanceUntilIdle()

        assertEquals(BrainState.CURIOUS, store.currentSnapshot().currentState)

        job.cancel()
    }

    @Test
    fun inactivity_transitionsToSleepyAfterThreshold() = runTest {
        var nowMs = 0L
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.IDLE, nowProvider = { nowMs })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { nowMs },
            inactivityThresholdMs = 60_000L
        )

        nowMs = 61_000L
        loop.checkInactivity(nowMs)

        assertEquals(BrainState.SLEEPY, store.currentSnapshot().currentState)
    }

    @Test
    fun petWhileCurious_transitionsToHappy() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.CURIOUS, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )
        val job = launch { loop.observeEventsAndApplyTransitions() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.USER_INTERACTED_PET,
                timestampMs = 3_000L,
                payloadJson = "{}"
            )
        )
        advanceUntilIdle()

        assertEquals(BrainState.HAPPY, store.currentSnapshot().currentState)

        job.cancel()
    }

    @Test
    fun wakeFromSleepy_onObjectStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.SLEEPY, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )
        val job = launch { loop.observeEventsAndApplyTransitions() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.OBJECT_DETECTED,
                timestampMs = 4_000L,
                payloadJson = "{}"
            )
        )
        advanceUntilIdle()

        assertEquals(BrainState.CURIOUS, store.currentSnapshot().currentState)

        job.cancel()
    }

    @Test
    fun noDuplicateStateChangedEvent_whenStateUnchanged() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.HAPPY, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )
        val job = launch { loop.observeEventsAndApplyTransitions() }

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 2_000L,
                payloadJson = "{}"
            )
        )
        advanceUntilIdle()

        assertFalse(
            eventBus.publishedEvents.count { it.type == EventType.PERSON_RECOGNIZED } > 1
        )

        job.cancel()
    }

    @Test
    fun forceSleepAndWake_applyTransitions() = runTest {
        val eventBus = FakeEventBus()
        val store = BrainStateStore(initialState = BrainState.IDLE, nowProvider = { 1_000L })
        val loop = BrainInteractionLoop(
            eventBus = eventBus,
            brainStateStore = store,
            nowProvider = { 1_000L }
        )

        loop.forceSleep(timestampMs = 2_000L)
        assertEquals(BrainState.SLEEPY, store.currentSnapshot().currentState)

        loop.forceWake(timestampMs = 3_000L)
        assertEquals(BrainState.CURIOUS, store.currentSnapshot().currentState)
    }
}

private class FakeEventBus : EventBus {
    private val flow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val publishedEvents = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
        flow.emit(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return flow.asSharedFlow()
    }
}
