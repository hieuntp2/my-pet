package com.aipet.brain.brain.observations

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ObservationRecorderTest {
    @Test
    fun recordPersonLikeObservation_publishesObservationEvent() = runBlocking {
        val fakeEventBus = FakeEventBus()
        val recorder = ObservationRecorder(fakeEventBus)

        recorder.recordPersonLikeObservation(
            source = ObservationSource.DEBUG,
            note = "debug_test_note",
            observedAtMs = 1_700L
        )

        assertEquals(1, fakeEventBus.publishedEvents.size)
        val published = fakeEventBus.publishedEvents.single()
        assertEquals(EventType.PERCEPTION_OBSERVATION_RECORDED, published.type)
        assertEquals(1_700L, published.timestampMs)
        assertTrue(published.payloadJson.contains("\"source\":\"DEBUG\""))
        assertTrue(published.payloadJson.contains("\"observationType\":\"PERSON_LIKE\""))
    }

    @Test
    fun recordObservation_payloadDoesNotAutoLinkPersonIdentity() = runBlocking {
        val fakeEventBus = FakeEventBus()
        val recorder = ObservationRecorder(fakeEventBus)

        recorder.recordObservation(
            PerceptionObservation.create(
                source = ObservationSource.CAMERA,
                observationType = ObservationType.HUMAN_RELATED,
                note = "camera_frame_active",
                observedAtMs = 2_000L,
                observationId = "observation-1"
            )
        )

        val payloadJson = fakeEventBus.publishedEvents.single().payloadJson
        assertFalse(payloadJson.contains("personId"))
    }
}

private class FakeEventBus : EventBus {
    val publishedEvents = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return emptyFlow()
    }
}
