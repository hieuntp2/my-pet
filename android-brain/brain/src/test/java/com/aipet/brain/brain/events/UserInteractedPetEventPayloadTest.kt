package com.aipet.brain.brain.events

import com.aipet.brain.brain.interaction.PetInteractionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UserInteractedPetEventPayloadTest {
    @Test
    fun `toJson and fromJson preserve interaction type`() {
        val payload = UserInteractedPetEventPayload(
            interactedAtMs = 123L,
            source = "home_pet_avatar_long_press",
            interactionType = PetInteractionType.LONG_PRESS.name
        )

        val json = payload.toJson()
        val parsed = UserInteractedPetEventPayload.fromJson(json)

        assertTrue(json.contains("\"interactionType\":\"LONG_PRESS\""))
        assertEquals(payload, parsed)
    }
}
