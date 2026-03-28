package com.aipet.brain.brain.events.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class LocalAudioIntentEventContractsTest {
    @Test
    fun `toJson and fromJson round-trip`() {
        val payload = LocalAudioIntentEvent(
            intent = AudioIntent.LEARN_PERSON,
            confidence = 0.35f,
            rawText = "learn person"
        )

        val parsed = LocalAudioIntentEvent.fromJson(payload.toJson())

        assertNotNull(parsed)
        assertEquals(AudioIntent.LEARN_PERSON, parsed?.intent)
        assertEquals(0.35f, parsed?.confidence)
        assertEquals("learn person", parsed?.rawText)
    }

    @Test
    fun `fromJson returns null for blank raw text`() {
        val parsed = LocalAudioIntentEvent.fromJson(
            "{\"intent\":\"UNKNOWN\",\"confidence\":0.2,\"rawText\":\"  \"}"
        )

        assertNull(parsed)
    }
}
