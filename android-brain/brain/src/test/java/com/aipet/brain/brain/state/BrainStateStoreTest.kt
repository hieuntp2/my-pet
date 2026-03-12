package com.aipet.brain.brain.state

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrainStateStoreTest {
    @Test
    fun defaultState_isIdle() {
        val store = BrainStateStore(nowProvider = { 1_000L })

        val snapshot = store.currentSnapshot()

        assertEquals(BrainState.IDLE, snapshot.currentState)
        assertEquals(1_000L, snapshot.updatedAtMs)
    }

    @Test
    fun setState_updatesSnapshot_whenStateChanges() = runTest {
        val store = BrainStateStore(nowProvider = { 2_000L })

        val changed = store.setState(targetState = BrainState.HAPPY, timestampMs = 3_000L)

        assertTrue(changed)
        assertEquals(BrainState.HAPPY, store.currentSnapshot().currentState)
        assertEquals(3_000L, store.currentSnapshot().updatedAtMs)
    }

    @Test
    fun setState_publishesBrainStateChangedEvent_whenStateChanges() = runTest {
        val eventBus = RecordingEventBus()
        val store = BrainStateStore(
            nowProvider = { 2_000L },
            eventBus = eventBus
        )

        val changed = store.setState(
            targetState = BrainState.HAPPY,
            timestampMs = 3_000L,
            reason = "PERSON_RECOGNIZED"
        )

        assertTrue(changed)
        assertEquals(1, eventBus.publishedEvents.size)

        val event = eventBus.publishedEvents.single()
        assertEquals(EventType.BRAIN_STATE_CHANGED, event.type)
        assertEquals(3_000L, event.timestampMs)
        assertTrue(event.payloadJson.contains("\"fromState\":\"IDLE\""))
        assertTrue(event.payloadJson.contains("\"toState\":\"HAPPY\""))
        assertTrue(event.payloadJson.contains("\"reason\":\"PERSON_RECOGNIZED\""))
        assertTrue(event.payloadJson.contains("\"changedAtMs\":3000"))
    }

    @Test
    fun setState_returnsFalse_whenStateIsUnchanged() = runTest {
        val eventBus = RecordingEventBus()
        val store = BrainStateStore(
            initialState = BrainState.CURIOUS,
            nowProvider = { 4_000L },
            eventBus = eventBus
        )

        val changed = store.setState(
            targetState = BrainState.CURIOUS,
            timestampMs = 5_000L,
            reason = "UNCHANGED"
        )

        assertFalse(changed)
        assertEquals(BrainState.CURIOUS, store.currentSnapshot().currentState)
        assertEquals(4_000L, store.currentSnapshot().updatedAtMs)
        assertTrue(eventBus.publishedEvents.isEmpty())
    }
}

private class RecordingEventBus : EventBus {
    val publishedEvents = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return emptyFlow()
    }
}
