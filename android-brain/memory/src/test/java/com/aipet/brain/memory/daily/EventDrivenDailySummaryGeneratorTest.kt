package com.aipet.brain.memory.daily

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetMood
import com.aipet.brain.brain.pet.PetState
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventDrivenDailySummaryGeneratorTest {
    private val generator = EventDrivenDailySummaryGenerator(zoneId = ZoneOffset.UTC)

    @Test
    fun generateForDate_returnsNullForSparseSingleTapDay() {
        val targetDate = LocalDate.of(2026, 3, 18)
        val summary = generator.generateForDate(
            targetDate = targetDate,
            persistedEvents = listOf(
                EventEnvelope.create(
                    type = EventType.USER_INTERACTED_PET,
                    payloadJson = UserInteractedPetEventPayload(
                        interactedAtMs = 1_000L,
                        source = "home",
                        interactionType = PetInteractionType.TAP.name
                    ).toJson(),
                    timestampMs = 1_000L
                )
            )
        )

        assertNull(summary)
    }

    @Test
    fun generateForDate_summarizesRealEventsAndState() {
        val targetDate = LocalDate.of(2026, 3, 18)
        val state = PetState(
            mood = PetMood.HAPPY,
            energy = 72,
            hunger = 25,
            sleepiness = 18,
            social = 60,
            bond = 20,
            lastUpdatedAt = 1_800_000L
        )
        val summary = generator.generateForDate(
            targetDate = targetDate,
            persistedEvents = listOf(
                EventEnvelope.create(
                    type = EventType.PET_GREETED,
                    payloadJson = PetGreetedEventPayload(
                        greetedAtMs = 1_000L,
                        emotion = "CURIOUS",
                        reason = "app_open",
                        message = "Hi again!"
                    ).toJson(),
                    timestampMs = 1_000L
                ),
                EventEnvelope.create(
                    type = EventType.PET_FED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "FEED",
                        actedAtMs = 1_200_000L,
                        reason = "feed",
                        resultingMood = "HAPPY",
                        energyDelta = 0,
                        hungerDelta = -20,
                        sleepinessDelta = 0,
                        socialDelta = 0,
                        bondDelta = 2
                    ).toJson(),
                    timestampMs = 1_200_000L
                ),
                EventEnvelope.create(
                    type = EventType.PET_PLAYED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "PLAY",
                        actedAtMs = 1_500_000L,
                        reason = "play",
                        resultingMood = "EXCITED",
                        energyDelta = -5,
                        hungerDelta = 0,
                        sleepinessDelta = 0,
                        socialDelta = 5,
                        bondDelta = 3
                    ).toJson(),
                    timestampMs = 1_500_000L
                )
            ),
            petStateSnapshot = state,
            continuityLabel = "New day"
        )

        requireNotNull(summary)
        assertEquals("A new day together", summary.title)
        assertEquals(0, summary.interactionCount)
        assertEquals(2, summary.activityCount)
        assertEquals("HAPPY", summary.dominantMood)
        assertTrue(summary.highlights.contains("New day"))
        assertTrue(summary.summary.contains("3 saved pet moments"))
        assertEquals(72, summary.energySnapshot)
    }

    @Test
    fun generateAll_returnsNewestDaysFirst() {
        val summaries = generator.generateAll(
            persistedEvents = listOf(
                EventEnvelope.create(
                    type = EventType.PET_FED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "FEED",
                        actedAtMs = 86_400_000L,
                        reason = "feed",
                        resultingMood = "HAPPY",
                        energyDelta = 0,
                        hungerDelta = -10,
                        sleepinessDelta = 0,
                        socialDelta = 0,
                        bondDelta = 1
                    ).toJson(),
                    timestampMs = 86_400_000L
                ),
                EventEnvelope.create(
                    type = EventType.PET_PLAYED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "PLAY",
                        actedAtMs = 86_460_000L,
                        reason = "play",
                        resultingMood = "EXCITED",
                        energyDelta = -5,
                        hungerDelta = 0,
                        sleepinessDelta = 0,
                        socialDelta = 5,
                        bondDelta = 2
                    ).toJson(),
                    timestampMs = 86_460_000L
                ),
                EventEnvelope.create(
                    type = EventType.PET_FED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "FEED",
                        actedAtMs = 1_000L,
                        reason = "feed",
                        resultingMood = "HAPPY",
                        energyDelta = 0,
                        hungerDelta = -10,
                        sleepinessDelta = 0,
                        socialDelta = 0,
                        bondDelta = 1
                    ).toJson(),
                    timestampMs = 1_000L
                ),
                EventEnvelope.create(
                    type = EventType.PET_RESTED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "REST",
                        actedAtMs = 2_000L,
                        reason = "rest",
                        resultingMood = "SLEEPY",
                        energyDelta = 10,
                        hungerDelta = 0,
                        sleepinessDelta = -15,
                        socialDelta = 0,
                        bondDelta = 1
                    ).toJson(),
                    timestampMs = 2_000L
                )
            )
        )

        assertEquals(2, summaries.size)
        assertTrue(summaries[0].date.isAfter(summaries[1].date))
    }
}
