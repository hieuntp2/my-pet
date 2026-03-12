package com.aipet.brain.brain.relationship

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonRecognizedPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.events.RelationshipUpdatedEventPayload
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
    fun personRecognizedEvent_increasesFamiliarityForRecognizedPerson() = runTest {
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
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 5_000L,
                payloadJson = PersonRecognizedPayload(
                    personId = "person-1",
                    similarityScore = 0.92f,
                    threshold = 0.75f,
                    evaluatedCandidates = 3,
                    timestamp = 5_000L
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(1, familiarityStore.updates.size)
        assertEquals("person-1", familiarityStore.updates.single().personId)
        assertEquals(FamiliarityEngine.DEFAULT_RECOGNITION_DELTA, familiarityStore.updates.single().delta)
        assertEquals(5_000L, familiarityStore.updates.single().updatedAtMs)
        val relationshipUpdatedEvents = eventBus.publishedEvents.filter { it.type == EventType.RELATIONSHIP_UPDATED }
        assertEquals(1, relationshipUpdatedEvents.size)
        val payload = RelationshipUpdatedEventPayload.fromJson(relationshipUpdatedEvents.single().payloadJson)
        assertEquals("person-1", payload?.personId)
        assertEquals(familiarityStore.updates.single().updatedFamiliarityScore, payload?.familiarityScore)
        job.cancel()
    }

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
        assertEquals(1, eventBus.publishedEvents.count { it.type == EventType.RELATIONSHIP_UPDATED })
        job.cancel()
    }

    @Test
    fun petEvent_afterPersonRecognized_increasesFamiliarityForCurrentRecognizedPerson() = runTest {
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
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 10_000L,
                payloadJson = PersonRecognizedPayload(
                    personId = "person-2",
                    similarityScore = 0.93f,
                    threshold = 0.75f,
                    evaluatedCandidates = 2,
                    timestamp = 10_000L
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
        assertEquals(2, eventBus.publishedEvents.count { it.type == EventType.RELATIONSHIP_UPDATED })
        job.cancel()
    }

    @Test
    fun petEvent_afterPersonUnknown_doesNotIncreaseFamiliarity() = runTest {
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
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 10_000L,
                payloadJson = PersonRecognizedPayload(
                    personId = "person-3",
                    similarityScore = 0.91f,
                    threshold = 0.75f,
                    evaluatedCandidates = 2,
                    timestamp = 10_000L
                ).toJson()
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN,
                timestampMs = 10_500L,
                payloadJson = "{\"source\":\"unit_test\"}"
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

        assertEquals(1, familiarityStore.updates.size)
        assertEquals("person-3", familiarityStore.updates.single().personId)
        assertEquals(FamiliarityEngine.DEFAULT_RECOGNITION_DELTA, familiarityStore.updates.single().delta)
        assertEquals(1, eventBus.publishedEvents.count { it.type == EventType.RELATIONSHIP_UPDATED })
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
        assertTrue(eventBus.publishedEvents.none { it.type == EventType.RELATIONSHIP_UPDATED })
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
        assertEquals(2, eventBus.publishedEvents.count { it.type == EventType.RELATIONSHIP_UPDATED })
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
        assertTrue(eventBus.publishedEvents.none { it.type == EventType.RELATIONSHIP_UPDATED })
        job.cancel()
    }

    @Test
    fun recognizedEvent_whenFamiliarityUnchanged_doesNotPublishRelationshipUpdated() = runTest {
        val eventBus = FakeEventBus()
        val familiarityStore = FakeFamiliarityStore(
            initialScores = mapOf("person-max" to 1.0f)
        )
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
                type = EventType.PERSON_RECOGNIZED,
                timestampMs = 15_000L,
                payloadJson = PersonRecognizedPayload(
                    personId = "person-max",
                    similarityScore = 0.95f,
                    threshold = 0.75f,
                    evaluatedCandidates = 1,
                    timestamp = 15_000L
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(1, familiarityStore.updates.size)
        assertEquals(1.0f, familiarityStore.updates.single().updatedFamiliarityScore)
        assertTrue(eventBus.publishedEvents.none { it.type == EventType.RELATIONSHIP_UPDATED })
        job.cancel()
    }
}

private class FakeFamiliarityStore(
    initialScores: Map<String, Float> = emptyMap()
) : FamiliarityStore {
    val updates = mutableListOf<FamiliarityUpdate>()
    private val familiarityScores = initialScores.toMutableMap()

    override suspend fun increaseFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): FamiliarityIncreaseResult? {
        if (!delta.isFinite()) {
            return null
        }
        val normalizedPersonId = personId.trim()
        if (normalizedPersonId.isBlank()) {
            return null
        }
        val previousFamiliarityScore = familiarityScores[normalizedPersonId] ?: 0f
        val updatedFamiliarityScore = (previousFamiliarityScore + delta).coerceIn(0f, 1f)
        familiarityScores[normalizedPersonId] = updatedFamiliarityScore
        updates.add(
            FamiliarityUpdate(
                personId = normalizedPersonId,
                delta = delta,
                updatedAtMs = updatedAtMs,
                previousFamiliarityScore = previousFamiliarityScore,
                updatedFamiliarityScore = updatedFamiliarityScore
            )
        )
        return FamiliarityIncreaseResult(
            personId = normalizedPersonId,
            previousFamiliarityScore = previousFamiliarityScore,
            updatedFamiliarityScore = updatedFamiliarityScore,
            updatedAtMs = updatedAtMs
        )
    }
}

private data class FamiliarityUpdate(
    val personId: String,
    val delta: Float,
    val updatedAtMs: Long,
    val previousFamiliarityScore: Float,
    val updatedFamiliarityScore: Float
)

private class FakeEventBus : EventBus {
    private val eventsFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 16
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
