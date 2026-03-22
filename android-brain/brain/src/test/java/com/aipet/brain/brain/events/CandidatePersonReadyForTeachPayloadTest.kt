package com.aipet.brain.brain.events

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CandidatePersonReadyForTeachPayloadTest {

    @Test
    fun `toJson and fromJson preserve payload fields`() {
        val payload = CandidatePersonReadyForTeachPayload(
            sessionId = "session-123",
            sampleCount = 6,
            stableScore = 0.82f,
            centroidEmbedding = listOf(0.1f, 0.2f, 0.3f),
            previewImageBase64 = "aGVsbG8=",
            readyAtMs = 123456L
        )

        val parsed = CandidatePersonReadyForTeachPayload.fromJson(payload.toJson())

        assertNotNull(parsed)
        assertEquals("session-123", parsed?.sessionId)
        assertEquals(6, parsed?.sampleCount)
        assertEquals(0.82f, parsed?.stableScore ?: 0f, 0f)
        assertEquals(3, parsed?.centroidEmbedding?.size)
        assertEquals(0.2f, parsed?.centroidEmbedding?.get(1) ?: 0f, 0f)
        assertEquals("aGVsbG8=", parsed?.previewImageBase64)
        assertEquals(123456L, parsed?.readyAtMs)
    }

    @Test
    fun `fromJson accepts null preview image`() {
        val json = """
            {
              "sessionId":"s-1",
              "sampleCount":5,
              "stableScore":0.77,
              "centroidEmbedding":[0.4,0.5],
              "previewImageBase64":null,
              "readyAtMs":111
            }
        """.trimIndent()

        val parsed = CandidatePersonReadyForTeachPayload.fromJson(json)

        assertNotNull(parsed)
        assertNull(parsed?.previewImageBase64)
        assertEquals(2, parsed?.centroidEmbedding?.size)
    }
}
