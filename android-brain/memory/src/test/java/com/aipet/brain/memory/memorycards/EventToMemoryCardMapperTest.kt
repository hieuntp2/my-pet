package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.memory.MemoryCardImportance
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EventToMemoryCardMapperTest {
    private val mapper = EventToMemoryCardMapper(zoneId = ZoneOffset.UTC)

    @Test
    fun map_returnsReadableGreetingCard() {
        val event = EventEnvelope.create(
            type = EventType.PET_GREETED,
            payloadJson = PetGreetedEventPayload(
                greetedAtMs = 1_000L,
                emotion = "HAPPY",
                reason = "app_open",
                message = "Hi again!"
            ).toJson(),
            timestampMs = 1_000L,
            eventId = "greeting-1"
        )

        val card = mapper.map(event)

        requireNotNull(card)
        assertEquals("A warm hello", card.title)
        assertEquals("Hi again!", card.summary)
        assertEquals(EventType.PET_GREETED, card.sourceEventType)
    }

    @Test
    fun map_returnsReadableTapCard() {
        val event = EventEnvelope.create(
            type = EventType.USER_INTERACTED_PET,
            payloadJson = UserInteractedPetEventPayload(
                interactedAtMs = 2_000L,
                source = "home",
                interactionType = PetInteractionType.TAP.name,
                resultingMood = "HAPPY",
                socialDelta = 1,
                bondDelta = 1,
                feedbackText = "Cún enjoyed that little hello"
            ).toJson(),
            timestampMs = 2_000L,
            eventId = "tap-1"
        )

        val card = mapper.map(event)

        requireNotNull(card)
        assertEquals("A quick hello", card.title)
        assertEquals("Cún enjoyed that little hello.", card.summary)
    }

    @Test
    fun map_returnsReadableFeedCard() {
        val event = EventEnvelope.create(
            type = EventType.PET_FED,
            payloadJson = PetActivityAppliedEventPayload(
                activityType = "FEED",
                actedAtMs = 3_000L,
                reason = "home_feed_button",
                resultingMood = "HAPPY",
                energyDelta = 0,
                hungerDelta = -25,
                sleepinessDelta = 0,
                socialDelta = 0,
                bondDelta = 2,
                feedbackText = "Cún seems satisfied now"
            ).toJson(),
            timestampMs = 3_000L,
            eventId = "feed-1"
        )

        val card = mapper.map(event)

        requireNotNull(card)
        assertEquals("Shared mealtime", card.title)
        assertTrue(card.summary.contains("satisfied", ignoreCase = true))
    }

    @Test
    fun map_returnsNullForUnsupportedEvents() {
        val event = EventEnvelope.create(type = EventType.TEST_EVENT, eventId = "ignored")

        val card = mapper.map(event)

        assertNull(card)
    }

    @Test
    fun mapEvents_marks_first_interaction_long_absence_and_high_activity_as_notable() {
        val cards = mapper.mapEvents(
            listOf(
                EventEnvelope.create(
                    type = EventType.USER_INTERACTED_PET,
                    payloadJson = UserInteractedPetEventPayload(
                        interactedAtMs = 1_000L,
                        source = "home",
                        interactionType = PetInteractionType.TAP.name,
                        resultingMood = "HAPPY",
                        socialDelta = 1,
                        bondDelta = 1,
                        feedbackText = "Cún enjoyed that little hello"
                    ).toJson(),
                    timestampMs = 1_000L,
                    eventId = "tap-first"
                ),
                EventEnvelope.create(
                    type = EventType.PET_PLAYED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "PLAY",
                        actedAtMs = 5_000L,
                        reason = "play",
                        resultingMood = "EXCITED",
                        energyDelta = -5,
                        hungerDelta = 0,
                        sleepinessDelta = 0,
                        socialDelta = 5,
                        bondDelta = 2,
                        feedbackText = "Cún had fun playing with you"
                    ).toJson(),
                    timestampMs = 5_000L,
                    eventId = "play-1"
                ),
                EventEnvelope.create(
                    type = EventType.PET_PLAYED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "PLAY",
                        actedAtMs = 8_000L,
                        reason = "play",
                        resultingMood = "EXCITED",
                        energyDelta = -5,
                        hungerDelta = 0,
                        sleepinessDelta = 0,
                        socialDelta = 5,
                        bondDelta = 2,
                        feedbackText = "Cún was ready for more fun"
                    ).toJson(),
                    timestampMs = 8_000L,
                    eventId = "play-2"
                ),
                EventEnvelope.create(
                    type = EventType.PET_FED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "FEED",
                        actedAtMs = 10_000L,
                        reason = "feed",
                        resultingMood = "HAPPY",
                        energyDelta = 0,
                        hungerDelta = -25,
                        sleepinessDelta = 0,
                        socialDelta = 0,
                        bondDelta = 1,
                        feedbackText = "Cún seems satisfied now"
                    ).toJson(),
                    timestampMs = 10_000L,
                    eventId = "feed-first"
                ),
                EventEnvelope.create(
                    type = EventType.PET_GREETED,
                    payloadJson = PetGreetedEventPayload(
                        greetedAtMs = 30_000_000L,
                        emotion = "CURIOUS",
                        reason = "return",
                        message = "You're back!"
                    ).toJson(),
                    timestampMs = 30_000_000L,
                    eventId = "greeting-return"
                )
            )
        )

        val firstTap = cards.first { it.id == "tap-first" }
        val feedCard = cards.first { it.id == "feed-first" }
        val streakCard = cards.first { it.id == "play-2" }
        val greetingCard = cards.first { it.id == "greeting-return" }

        assertEquals(MemoryCardImportance.NOTABLE, firstTap.importance)
        assertEquals("First hello today", firstTap.notableMomentLabel)
        assertEquals(MemoryCardImportance.NOTABLE, feedCard.importance)
        assertEquals("First meal today", feedCard.notableMomentLabel)
        assertEquals(MemoryCardImportance.NOTABLE, streakCard.importance)
        assertEquals("Playful streak", streakCard.notableMomentLabel)
        assertEquals(MemoryCardImportance.NOTABLE, greetingCard.importance)
        assertEquals("Back together", greetingCard.notableMomentLabel)
    }
}
