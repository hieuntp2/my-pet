package com.aipet.brain.brain.memory

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.ObjectDetectedEventPayload
import com.aipet.brain.brain.events.PersonRecognizedPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.events.audio.SoundEnergyPayload
import com.aipet.brain.brain.events.audio.VoiceActivityPayload
import com.aipet.brain.brain.events.audio.VoiceActivityState
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
    fun personRecognizedEvent_updatesCurrentPersonAndLastStimulus() = runTest {
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
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 1_000L,
                payloadJson = PersonRecognizedPayload(
                    personId = "person-1",
                    similarityScore = 0.9f,
                    threshold = 0.75f,
                    evaluatedCandidates = 3,
                    timestamp = 1_000L
                ).toJson()
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("person-1", memory.currentPersonId)
        assertEquals(1_000L, memory.lastStimulusTs)
        job.cancel()
    }

    @Test
    fun personSeenRecordedEvent_stillUpdatesCurrentPersonAndLastStimulus() = runTest {
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
                timestampMs = 2_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-2",
                    seenAtMs = 2_000L,
                    seenCount = 1,
                    isOwner = false,
                    source = "unit_test"
                ).toJson()
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("person-2", memory.currentPersonId)
        assertEquals(2_000L, memory.lastStimulusTs)
        job.cancel()
    }

    @Test
    fun objectDetectedEvent_usesObjectIdAndUpdatesLastStimulus() = runTest {
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
                timestampMs = 2_500L,
                payloadJson = ObjectDetectedEventPayload(
                    objectId = "object-1",
                    label = "ball",
                    confidence = 0.91f,
                    detectedAtMs = 2_500L
                ).toJson()
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("object-1", memory.currentObjectId)
        assertEquals(2_500L, memory.lastStimulusTs)
        job.cancel()
    }

    @Test
    fun personUnknownEvent_clearsCurrentPerson() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(
                currentPersonId = "person-1",
                currentObjectId = "object-1",
                lastStimulusTs = 500L
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
                type = EventType.PERSON_UNKNOWN,
                timestampMs = 3_000L,
                payloadJson = "{\"bestScore\":0.2,\"threshold\":0.5,\"evaluatedCandidates\":2,\"timestamp\":3000}"
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals(null, memory.currentPersonId)
        assertEquals("object-1", memory.currentObjectId)
        assertEquals(3_000L, memory.lastStimulusTs)
        job.cancel()
    }

    @Test
    fun petInteractionEvent_updatesLastStimulusOnly() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(
                currentPersonId = "person-1",
                currentObjectId = "object-2",
                lastStimulusTs = 1_000L
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
        assertEquals("object-2", memory.currentObjectId)
        assertEquals(4_000L, memory.lastStimulusTs)
        job.cancel()
    }

    @Test
    fun strongSoundEvent_updatesLastStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(lastStimulusTs = 1_000L)
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
                type = EventType.SOUND_DETECTED,
                timestampMs = 6_000L,
                payloadJson = SoundEnergyPayload(
                    rms = 0.12,
                    peak = 0.52,
                    smoothedEnergy = 0.27,
                    timestamp = 6_000L,
                    kind = "CLAP"
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(6_000L, store.currentSnapshot().lastStimulusTs)
        job.cancel()
    }

    @Test
    fun weakSoundEvent_doesNotUpdateLastStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(lastStimulusTs = 2_000L)
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
                type = EventType.SOUND_DETECTED,
                timestampMs = 7_000L,
                payloadJson = SoundEnergyPayload(
                    rms = 0.05,
                    peak = 0.2,
                    smoothedEnergy = 0.1,
                    timestamp = 7_000L,
                    kind = "AMBIENT"
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(2_000L, store.currentSnapshot().lastStimulusTs)
        job.cancel()
    }

    @Test
    fun voiceActivityStarted_updatesLastStimulus() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(lastStimulusTs = 3_000L)
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
                type = EventType.VOICE_ACTIVITY_STARTED,
                timestampMs = 8_000L,
                payloadJson = VoiceActivityPayload(
                    state = VoiceActivityState.STARTED,
                    confidence = 0.4f,
                    timestamp = 8_000L,
                    vadState = "ACTIVE"
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(8_000L, store.currentSnapshot().lastStimulusTs)
        job.cancel()
    }

    @Test
    fun longPressEvent_updatesLastStimulusOnly() = runTest {
        val eventBus = FakeEventBus()
        val store = WorkingMemoryStore(
            initialMemory = WorkingMemory(
                currentPersonId = "person-9",
                currentObjectId = "object-4",
                lastStimulusTs = 2_000L
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
                type = EventType.PET_LONG_PRESSED,
                timestampMs = 5_000L,
                payloadJson = "{\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        val memory = store.currentSnapshot()
        assertEquals("person-9", memory.currentPersonId)
        assertEquals("object-4", memory.currentObjectId)
        assertEquals(5_000L, memory.lastStimulusTs)
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
