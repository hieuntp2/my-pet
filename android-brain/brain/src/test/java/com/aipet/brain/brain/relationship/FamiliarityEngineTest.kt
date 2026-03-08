package com.aipet.brain.brain.relationship

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
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class FamiliarityEngineTest {
    @Test
    fun personSeenEvent_increasesFamiliarityForRecognizedPerson() = runTest {
        val eventBus = FakeEventBus()
        val familiarityStore = FakeFamiliarityStore()
        val engine = FamiliarityEngine(
            eventBus = eventBus,
            familiarityStore = familiarityStore
        )
        val job = launch {
            engine.observeEventsAndApplyRules()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 5_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-1",
                    seenAtMs = 5_000L,
                    seenCount = 2,
                    isOwner = false,
                    source = "unit_test"
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(1, familiarityStore.updates.size)
        assertEquals("person-1", familiarityStore.updates.single().personId)
        assertEquals(FamiliarityEngine.DEFAULT_RECOGNITION_DELTA, familiarityStore.updates.single().delta)
        assertEquals(5_000L, familiarityStore.updates.single().updatedAtMs)
        job.cancel()
    }

    @Test
    fun nonRecognitionEvents_doNotIncreaseFamiliarity() = runTest {
        val eventBus = FakeEventBus()
        val familiarityStore = FakeFamiliarityStore()
        val engine = FamiliarityEngine(
            eventBus = eventBus,
            familiarityStore = familiarityStore
        )
        val job = launch {
            engine.observeEventsAndApplyRules()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.TEST_EVENT,
                timestampMs = 8_000L,
                payloadJson = "{\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        assertTrue(familiarityStore.updates.isEmpty())
        job.cancel()
    }

    @Test
    fun petEvent_increasesFamiliarityForCurrentRecognizedPerson() = runTest {
        val eventBus = FakeEventBus()
        val familiarityStore = FakeFamiliarityStore()
        val engine = FamiliarityEngine(
            eventBus = eventBus,
            familiarityStore = familiarityStore
        )
        val job = launch {
            engine.observeEventsAndApplyRules()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 10_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-2",
                    seenAtMs = 10_000L,
                    seenCount = 1,
                    isOwner = false,
                    source = "unit_test"
                ).toJson()
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.USER_INTERACTED_PET,
                timestampMs = 11_000L,
                payloadJson = "{\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        assertEquals(2, familiarityStore.updates.size)
        val petUpdate = familiarityStore.updates.last()
        assertEquals("person-2", petUpdate.personId)
        assertEquals(FamiliarityEngine.DEFAULT_PET_DELTA, petUpdate.delta)
        assertEquals(11_000L, petUpdate.updatedAtMs)
        job.cancel()
    }

    @Test
    fun petEvent_withoutRecognizedPersonContext_doesNotIncreaseFamiliarity() = runTest {
        val eventBus = FakeEventBus()
        val familiarityStore = FakeFamiliarityStore()
        val engine = FamiliarityEngine(
            eventBus = eventBus,
            familiarityStore = familiarityStore
        )
        val job = launch {
            engine.observeEventsAndApplyRules()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.USER_INTERACTED_PET,
                timestampMs = 12_000L,
                payloadJson = "{\"source\":\"unit_test\"}"
            )
        )
        advanceUntilIdle()

        assertTrue(familiarityStore.updates.isEmpty())
        job.cancel()
    }
}

private class FakeFamiliarityStore : FamiliarityStore {
    val updates = mutableListOf<FamiliarityUpdate>()

    override suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): Boolean {
        updates.add(
            FamiliarityUpdate(
                personId = personId,
                delta = delta,
                updatedAtMs = updatedAtMs
            )
        )
        return true
    }
}

private data class FamiliarityUpdate(
    val personId: String,
    val delta: Float,
    val updatedAtMs: Long
)

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
