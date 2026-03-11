package com.aipet.brain.brain.recognition

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.recognition.model.RecognitionClassification
import com.aipet.brain.brain.recognition.model.RecognitionResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class RecognitionDecisionEventPublisherTest {

    @Test
    fun publish_acceptedResult_emitsPersonRecognized_andUpdatesMemoryStats() = runTest {
        val eventBus = RecordingEventBus()
        val memoryUpdater = RecordingMemoryStatsUpdater()
        val publisher = RecognitionDecisionEventPublisher(
            eventBus = eventBus,
            recognitionMemoryStatsUpdater = memoryUpdater
        )
        val result = RecognitionResult(
            classification = RecognitionClassification.RECOGNIZED,
            bestPersonId = "person-1",
            bestScore = 0.84f,
            threshold = 0.75f,
            accepted = true,
            evaluatedCandidates = 3,
            timestamp = 1000L
        )

        publisher.publish(recognitionResult = result)

        assertEquals(1, memoryUpdater.calls.size)
        assertEquals("person-1", memoryUpdater.calls.single().personId)
        assertEquals(1000L, memoryUpdater.calls.single().timestampMs)
        assertEquals(1, eventBus.events.size)
        assertEquals(EventType.PERSON_RECOGNIZED, eventBus.events.single().type)
        assertTrue(eventBus.events.single().payloadJson.contains("\"personId\":\"person-1\""))
        assertTrue(eventBus.events.single().payloadJson.contains("\"threshold\":0.75"))
        assertTrue(eventBus.events.single().payloadJson.contains("\"evaluatedCandidates\":3"))
        assertTrue(eventBus.events.single().payloadJson.contains("\"timestamp\":1000"))
    }

    @Test
    fun publish_rejectedResult_emitsPersonUnknown_withoutMemoryStatsUpdate() = runTest {
        val eventBus = RecordingEventBus()
        val memoryUpdater = RecordingMemoryStatsUpdater()
        val logs = mutableListOf<String>()
        val publisher = RecognitionDecisionEventPublisher(
            eventBus = eventBus,
            recognitionMemoryStatsUpdater = memoryUpdater,
            updateLogger = { message ->
                logs += message
            }
        )
        val result = RecognitionResult(
            classification = RecognitionClassification.UNKNOWN,
            bestPersonId = null,
            bestScore = 0.70f,
            threshold = 0.75f,
            accepted = false,
            evaluatedCandidates = 1,
            timestamp = 2000L
        )

        publisher.publish(recognitionResult = result)

        assertEquals(0, memoryUpdater.calls.size)
        assertEquals(1, eventBus.events.size)
        assertEquals(EventType.PERSON_UNKNOWN, eventBus.events.single().type)
        assertTrue(eventBus.events.single().payloadJson.contains("\"bestScore\":0.7"))
        assertTrue(eventBus.events.single().payloadJson.contains("\"threshold\":0.75"))
        assertTrue(eventBus.events.single().payloadJson.contains("\"evaluatedCandidates\":1"))
        assertTrue(eventBus.events.single().payloadJson.contains("\"timestamp\":2000"))
        assertTrue(logs.any { it.contains("Recognition unknown:") })
        assertTrue(logs.any { it.contains("publishedEvent=PERSON_UNKNOWN") })
    }

    @Test
    fun publish_duplicateAcceptedResult_updatesMemoryStatsOnlyOnce() = runTest {
        val eventBus = RecordingEventBus()
        val memoryUpdater = RecordingMemoryStatsUpdater()
        val publisher = RecognitionDecisionEventPublisher(
            eventBus = eventBus,
            recognitionMemoryStatsUpdater = memoryUpdater
        )
        val result = RecognitionResult(
            classification = RecognitionClassification.RECOGNIZED,
            bestPersonId = "person-1",
            bestScore = 0.82f,
            threshold = 0.75f,
            accepted = true,
            evaluatedCandidates = 2,
            timestamp = 3000L
        )

        publisher.publish(recognitionResult = result)
        publisher.publish(recognitionResult = result)

        assertEquals(1, memoryUpdater.calls.size)
        assertEquals(2, eventBus.events.size)
        assertEquals(EventType.PERSON_RECOGNIZED, eventBus.events.first().type)
        assertEquals(EventType.PERSON_RECOGNIZED, eventBus.events.last().type)
    }

    @Test
    fun publish_unknownWithoutCandidates_emitsPersonUnknown_andLogsNoCandidatesReason() = runTest {
        val eventBus = RecordingEventBus()
        val memoryUpdater = RecordingMemoryStatsUpdater()
        val logs = mutableListOf<String>()
        val publisher = RecognitionDecisionEventPublisher(
            eventBus = eventBus,
            recognitionMemoryStatsUpdater = memoryUpdater,
            updateLogger = { message ->
                logs += message
            }
        )
        val result = RecognitionResult(
            classification = RecognitionClassification.UNKNOWN,
            bestPersonId = null,
            bestScore = 0f,
            threshold = 0.75f,
            accepted = false,
            evaluatedCandidates = 0,
            timestamp = 2100L
        )

        publisher.publish(recognitionResult = result)

        assertEquals(0, memoryUpdater.calls.size)
        assertEquals(1, eventBus.events.size)
        assertEquals(EventType.PERSON_UNKNOWN, eventBus.events.single().type)
        assertTrue(logs.any { it.contains("reason=no_candidates") })
    }
}

private class RecordingEventBus : EventBus {
    val events = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        events += event
    }

    override fun observe(): Flow<EventEnvelope> {
        return emptyFlow()
    }
}

private class RecordingMemoryStatsUpdater : RecognitionMemoryStatsUpdater {
    data class Call(
        val personId: String,
        val timestampMs: Long
    )

    val calls = mutableListOf<Call>()

    override suspend fun updatePersonSeenStats(
        personId: String,
        timestampMs: Long
    ): RecognitionMemoryStatsUpdate? {
        calls += Call(
            personId = personId,
            timestampMs = timestampMs
        )
        return RecognitionMemoryStatsUpdate(
            personId = personId,
            timestampMs = timestampMs,
            seenCount = calls.size
        )
    }
}
