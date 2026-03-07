package com.aipet.brain.app.ui.debug

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ObservationEventMapperTest {
    @Test
    fun fromEvent_parsesValidObservationPayload() {
        val event = EventEnvelope.create(
            type = EventType.PERCEPTION_OBSERVATION_RECORDED,
            timestampMs = 1_111L,
            payloadJson = """
                {
                  "observationId": "obs-1",
                  "observedAtMs": 2222,
                  "source": "CAMERA",
                  "observationType": "PERSON_LIKE",
                  "note": "frame=1280x720"
                }
            """.trimIndent()
        )

        val mapped = ObservationEventMapper.fromEvent(event)

        assertEquals("obs-1", mapped?.observationId)
        assertEquals(2_222L, mapped?.observedAtMs)
        assertEquals("CAMERA", mapped?.source)
        assertEquals("PERSON_LIKE", mapped?.observationType)
        assertEquals("frame=1280x720", mapped?.note)
    }

    @Test
    fun listRecent_filtersToObservationEventsAndPreservesOrder() {
        val latestObservation = EventEnvelope.create(
            type = EventType.PERCEPTION_OBSERVATION_RECORDED,
            timestampMs = 3_000L,
            payloadJson = """
                {"observationId":"obs-latest","observedAtMs":3000,"source":"DEBUG","observationType":"PERSON_LIKE","note":"latest"}
            """.trimIndent()
        )
        val nonObservation = EventEnvelope.create(
            type = EventType.TEST_EVENT,
            timestampMs = 2_000L
        )
        val olderObservation = EventEnvelope.create(
            type = EventType.PERCEPTION_OBSERVATION_RECORDED,
            timestampMs = 1_000L,
            payloadJson = """
                {"observationId":"obs-older","observedAtMs":1000,"source":"CAMERA","observationType":"HUMAN_RELATED","note":null}
            """.trimIndent()
        )

        val recent = ObservationEventMapper.listRecent(
            events = listOf(latestObservation, nonObservation, olderObservation),
            limit = 10
        )

        assertEquals(listOf("obs-latest", "obs-older"), recent.map { it.observationId })
    }

    @Test
    fun fromEvent_returnsNullForMalformedObservationPayload() {
        val malformed = EventEnvelope.create(
            type = EventType.PERCEPTION_OBSERVATION_RECORDED,
            payloadJson = "{\"observationId\": \"obs-1\", \"source\": \"CAMERA\""
        )

        val mapped = ObservationEventMapper.fromEvent(malformed)

        assertNull(mapped)
    }
}
