package com.aipet.brain.brain.memory

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WorkingMemoryUpdaterTest {
    @Test
    fun personSeenEvent_updatesCurrentPersonAndLastStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore()
        val updater = WorkingMemoryUpdater(
            eventBus = eventBus,
            workingMemoryStore = store
        )
        val job = launch {
            updater.observeEventsAndUpdateMemory()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 1_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-1",
                    seenAtMs = 1_000L,
                    seenCount = 1,
                    isOwner = false,
                    source = "unit_test"
                ).toJson()
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("person-1", memory.currentPersonId)
        assertEquals(1_000L, memory.lastStimulusAtMs)
        job.cancel()
    }

    @Test
    fun objectDetectedEvent_updatesCurrentObjectAndLastStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore()
        val updater = WorkingMemoryUpdater(
            eventBus = eventBus,
            workingMemoryStore = store
        )
        val job = launch {
            updater.observeEventsAndUpdateMemory()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.OBJECT_DETECTED,
                timestampMs = 2_000L,
                payloadJson = ObjectDetectedEventPayload(
                    label = "ball",
                    confidence = 0.91f,
                    detectedAtMs = 2_000L
                ).toJson()
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("ball", memory.currentObjectLabel)
        assertEquals(2_000L, memory.lastStimulusAtMs)
        job.cancel()
    }

    @Test
    fun personUnknownEvent_clearsCurrentPerson() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(
                currentPersonId = "person-1",
                currentObjectLabel = "ball",
                lastStimulusAtMs = 500L
            )
        )
        val updater = WorkingMemoryUpdater(
            eventBus = eventBus,
            workingMemoryStore = store
        )
        val job = launch {
            updater.observeEventsAndUpdateMemory()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN_DETECTED,
                timestampMs = 3_000L,
                payloadJson = "{\"seenAtMs\":3000,\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals(null, memory.currentPersonId)
        assertEquals("ball", memory.currentObjectLabel)
        assertEquals(3_000L, memory.lastStimulusAtMs)
        job.cancel()
    }

    @Test
    fun petInteractionEvent_updatesLastStimulusOnly() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(
                currentPersonId = "person-1",
                currentObjectLabel = "ball",
                lastStimulusAtMs = 1_000L
            )
        )
        val updater = WorkingMemoryUpdater(
            eventBus = eventBus,
            workingMemoryStore = store
        )
        val job = launch {
            updater.observeEventsAndUpdateMemory()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.USER_INTERACTED_PET,
                timestampMs = 4_000L,
                payloadJson = "{\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("person-1", memory.currentPersonId)
        assertEquals("ball", memory.currentObjectLabel)
        assertEquals(4_000L, memory.lastStimulusAtMs)
        job.cancel()
    }
}

private class FakeEventBus : EventBus {
    private val eventsFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 16
    )

    override suspend fun publish(event: EventEnvelope) {
        eventsFlow.emit(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return eventsFlow.asSharedFlow()
    }
}
