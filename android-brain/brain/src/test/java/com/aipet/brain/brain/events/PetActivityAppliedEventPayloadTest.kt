package com.aipet.brain.brain.events

import org.junit.Assert.assertTrue
import org.junit.Test

class PetActivityAppliedEventPayloadTest {
    @Test
    fun `toJson includes activity metadata and deltas`() {
        val json = PetActivityAppliedEventPayload(
            activityType = "FEED",
            actedAtMs = 123L,
            reason = "hungry_relief",
            resultingMood = "HAPPY",
            energyDelta = 0,
            hungerDelta = -25,
            sleepinessDelta = 0,
            socialDelta = 0,
            bondDelta = 1
        ).toJson()

        assertTrue(json.contains("\"activityType\":\"FEED\""))
        assertTrue(json.contains("\"reason\":\"hungry_relief\""))
        assertTrue(json.contains("\"resultingMood\":\"HAPPY\""))
        assertTrue(json.contains("\"hungerDelta\":-25"))
        assertTrue(json.contains("\"bondDelta\":1"))
    }
}
