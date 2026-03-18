package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.memory.MemoryCardImportance
import com.aipet.brain.brain.interaction.PetInteractionType
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
        assertEquals("Hi again!", card.title)
        assertTrue(card.summary.contains("happy"))
        assertEquals(EventType.PET_GREETED, card.sourceEventType)
    }

    @Test
    fun map_returnsReadableTapCard() {
        val event = EventEnvelope.create(
            type = EventType.USER_INTERACTED_PET,
            payloadJson = UserInteractedPetEventPayload(
                interactedAtMs = 2_000L,
                source = "home",
                interactionType = PetInteractionType.TAP.name
            ).toJson(),
            timestampMs = 2_000L,
            eventId = "tap-1"
        )

        val card = mapper.map(event)

        requireNotNull(card)
        assertEquals("A quick pet", card.title)
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
                bondDelta = 2
            ).toJson(),
            timestampMs = 3_000L,
            eventId = "feed-1"
        )

        val card = mapper.map(event)

        requireNotNull(card)
        assertEquals("Mealtime", card.title)
        assertTrue(card.summary.contains("happy"))
    }

    @Test
    fun map_returnsNullForUnsupportedEvents() {
        val event = EventEnvelope.create(type = EventType.TEST_EVENT, eventId = "ignored")

        val card = mapper.map(event)

        assertNull(card)
    }

    @Test
    fun mapEvents_marksFirstFeedAndLongAbsenceGreetingAsNotable() {
        val cards = mapper.mapEvents(
            listOf(
                EventEnvelope.create(
                    type = EventType.PET_FED,
                    payloadJson = PetActivityAppliedEventPayload(
                        activityType = "FEED",
                        actedAtMs = 1_000L,
                        reason = "feed",
                        resultingMood = "HAPPY",
                        energyDelta = 0,
                        hungerDelta = -20,
                        sleepinessDelta = 0,
                        socialDelta = 0,
                        bondDelta = 1
                    ).toJson(),
                    timestampMs = 1_000L,
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

        val feedCard = cards.first { it.id == "feed-first" }
        val greetingCard = cards.first { it.id == "greeting-return" }

        assertEquals(MemoryCardImportance.NOTABLE, feedCard.importance)
        assertEquals("First meal today", feedCard.notableMomentLabel)
        assertEquals(MemoryCardImportance.NOTABLE, greetingCard.importance)
        assertEquals("Welcome back", greetingCard.notableMomentLabel)
    }
}
