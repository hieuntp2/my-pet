package com.aipet.brain.brain.events

import org.junit.Assert.assertTrue
import org.junit.Test

class PetGreetedEventPayloadTest {
    @Test
    fun `toJson contains emotion reason and message`() {
        val payload = PetGreetedEventPayload(
            greetedAtMs = 123L,
            emotion = "HAPPY",
            reason = "high_bond",
            message = "so happy to see you!"
        )

        val json = payload.toJson()

        assertTrue(json.contains("\"emotion\":\"HAPPY\""))
        assertTrue(json.contains("\"reason\":\"high_bond\""))
        assertTrue(json.contains("\"message\":\"so happy to see you!\""))
    }
}
