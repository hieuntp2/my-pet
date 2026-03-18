package com.aipet.brain.brain.traits

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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TraitsEngineTest {
    @Test
    fun initialize_createsDefaultSnapshot_whenNoExistingData() = runTest {
        val repository = FakeTraitsSnapshotRepository()
        val engine = TraitsEngine(
            repository = repository,
            eventBus = FakeEventBus(),
            nowProvider = { 1_000L },
            idProvider = { "default-snapshot" }
        )

        engine.initializeIfNeeded()

        val current = engine.observeTraits().value
        assertNotNull(current)
        assertEquals("default-snapshot", current?.snapshotId)
        assertEquals(1, repository.savedSnapshots.size)
    }

    @Test
    fun initialize_reusesExistingSnapshot_whenAvailable() = runTest {
        val existing = TraitsSnapshot(
            snapshotId = "existing",
            capturedAtMs = 2_000L,
            curiosity = 0.2f,
            sociability = 0.3f,
            energy = 0.4f,
            patience = 0.5f,
            boldness = 0.6f
        )
        val repository = FakeTraitsSnapshotRepository(existing)
        val engine = TraitsEngine(
            repository = repository,
            eventBus = FakeEventBus(),
            nowProvider = { 3_000L },
            idProvider = { "new-id" }
        )

        engine.initializeIfNeeded()

        assertEquals("existing", engine.observeTraits().value?.snapshotId)
        assertEquals(0, repository.savedSnapshots.size)
    }

    @Test
    fun petEvent_increasesSociabilityAndPublishesTraitsUpdated() = runTest {
        val eventBus = FakeEventBus()
        val repository = FakeTraitsSnapshotRepository(
            TraitsSnapshot(
                snapshotId = "s1",
                capturedAtMs = 1_000L,
                curiosity = 0.5f,
                sociability = 0.5f,
                energy = 0.5f,
                patience = 0.5f,
                boldness = 0.5f
            )
        )
        val engine = TraitsEngine(
            repository = repository,
            eventBus = eventBus,
            nowProvider = { 2_000L },
            idProvider = { "s2" }
        )
        engine.initializeIfNeeded()
        val job = launch { engine.observeEventsAndApplyRules() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.USER_INTERACTED_PET,
                timestampMs = 2_000L
            )
        )
        advanceUntilIdle()

        val updated = engine.observeTraits().value
        assertNotNull(updated)
        assertTrue(updated!!.sociability > 0.5f)
        assertTrue(eventBus.publishedEvents.any { it.type == EventType.TRAITS_UPDATED })

        job.cancel()
    }

    @Test
    fun longPressEvent_increasesSociabilityAndPublishesTraitsUpdated() = runTest {
        val eventBus = FakeEventBus()
        val repository = FakeTraitsSnapshotRepository(
            TraitsSnapshot(
                snapshotId = "s1",
                capturedAtMs = 1_000L,
                curiosity = 0.5f,
                sociability = 0.5f,
                energy = 0.5f,
                patience = 0.5f,
                boldness = 0.5f
            )
        )
        val engine = TraitsEngine(
            repository = repository,
            eventBus = eventBus,
            nowProvider = { 2_500L },
            idProvider = { "s2" }
        )
        engine.initializeIfNeeded()
        val job = launch { engine.observeEventsAndApplyRules() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PET_LONG_PRESSED,
                timestampMs = 2_500L
            )
        )
        advanceUntilIdle()

        val updated = engine.observeTraits().value
        assertNotNull(updated)
        assertTrue(updated!!.sociability > 0.5f)
        assertTrue(eventBus.publishedEvents.any { it.type == EventType.TRAITS_UPDATED })

        job.cancel()
    }

    @Test
    fun sleepyTransition_decreasesEnergy() = runTest {
        val eventBus = FakeEventBus()
        val repository = FakeTraitsSnapshotRepository(
            TraitsSnapshot(
                snapshotId = "s1",
                capturedAtMs = 1_000L,
                curiosity = 0.5f,
                sociability = 0.5f,
                energy = 0.8f,
                patience = 0.5f,
                boldness = 0.5f
            )
        )
        val engine = TraitsEngine(
            repository = repository,
            eventBus = eventBus,
            nowProvider = { 3_000L },
            idProvider = { "s2" }
        )
        engine.initializeIfNeeded()
        val job = launch { engine.observeEventsAndApplyRules() }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.BRAIN_STATE_CHANGED,
                timestampMs = 3_000L,
                payloadJson = "{\"toState\":\"SLEEPY\"}"
            )
        )
        advanceUntilIdle()

        val updated = engine.observeTraits().value
        assertNotNull(updated)
        assertTrue(updated!!.energy < 0.8f)

        job.cancel()
    }
}

private class FakeTraitsSnapshotRepository(
    initialSnapshot: TraitsSnapshot? = null
) : TraitsSnapshotRepository {
    private var latest: TraitsSnapshot? = initialSnapshot
    val savedSnapshots = mutableListOf<TraitsSnapshot>()

    override suspend fun save(snapshot: TraitsSnapshot) {
        latest = snapshot
        savedSnapshots.add(snapshot)
    }

    override suspend fun latest(): TraitsSnapshot? {
        return latest
    }

    override fun observeLatest(): kotlinx.coroutines.flow.Flow<TraitsSnapshot?> {
        return kotlinx.coroutines.flow.flowOf(latest)
    }
}

private class FakeEventBus : EventBus {
    private val eventsFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 64
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
