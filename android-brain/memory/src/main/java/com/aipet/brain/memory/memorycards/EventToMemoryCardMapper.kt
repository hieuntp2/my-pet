package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.activity.PetActivityType
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardCategory
import com.aipet.brain.brain.memory.MemoryCardImportance
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate

class EventToMemoryCardMapper(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val longAbsenceThresholdMs: Long = 6 * 60 * 60 * 1000L
) {
    fun map(event: EventEnvelope): MemoryCard? {
        return when (event.type) {
            EventType.PET_GREETED -> mapGreeting(event)
            EventType.USER_INTERACTED_PET,
            EventType.PET_LONG_PRESSED -> mapInteraction(event)
            EventType.PET_FED,
            EventType.PET_PLAYED,
            EventType.PET_RESTED -> mapActivity(event)
            else -> null
        }
    }

    fun mapEvents(events: List<EventEnvelope>): List<MemoryCard> {
        val supportedEvents = events
            .sortedBy { it.timestampMs }
            .mapNotNull(::map)
        if (supportedEvents.isEmpty()) {
            return emptyList()
        }

        val firstFeedDates = linkedSetOf<LocalDate>()
        val notableIds = linkedMapOf<String, String>()
        var previousMemoryTimestampMs: Long? = null

        supportedEvents.forEach { card ->
            when (card.sourceEventType) {
                EventType.PET_FED -> {
                    val cardDate = Instant.ofEpochMilli(card.timestampMs)
                        .atZone(zoneId)
                        .toLocalDate()
                    if (firstFeedDates.add(cardDate)) {
                        notableIds[card.id] = "First meal today"
                    }
                }

                EventType.PET_GREETED -> {
                    val previousTimestamp = previousMemoryTimestampMs
                    if (previousTimestamp != null && card.timestampMs - previousTimestamp >= longAbsenceThresholdMs) {
                        notableIds[card.id] = "Welcome back"
                    }
                }

                else -> Unit
            }

            if (card.summary.contains("sleepy", ignoreCase = true) ||
                card.summary.contains("hungry", ignoreCase = true) ||
                card.summary.contains("sad", ignoreCase = true) ||
                card.summary.contains("excited", ignoreCase = true)
            ) {
                notableIds.putIfAbsent(card.id, "Big feeling")
            }

            previousMemoryTimestampMs = card.timestampMs
        }

        return supportedEvents
            .map { card ->
                val label = notableIds[card.id]
                if (label == null) {
                    card
                } else {
                    card.copy(
                        importance = MemoryCardImportance.NOTABLE,
                        notableMomentLabel = label
                    )
                }
            }
            .sortedWith(compareByDescending<MemoryCard> { it.timestampMs }.thenBy { it.id })
    }

    private fun mapGreeting(event: EventEnvelope): MemoryCard? {
        val payload = PetGreetedEventPayload.fromJson(event.payloadJson) ?: return null
        val emotionSummary = payload.emotion.lowercase().replace('_', ' ')
        return MemoryCard(
            id = event.eventId,
            title = "${payload.message}",
            summary = "Your pet greeted you with a $emotionSummary mood.",
            timestampMs = event.timestampMs,
            sourceEventType = event.type,
            category = MemoryCardCategory.GREETING
        )
    }

    private fun mapInteraction(event: EventEnvelope): MemoryCard? {
        val payload = UserInteractedPetEventPayload.fromJson(event.payloadJson) ?: return null
        return when (PetInteractionType.fromRawValue(payload.interactionType)) {
            PetInteractionType.LONG_PRESS -> MemoryCard(
                id = event.eventId,
                title = "A long cuddle",
                summary = "You stayed close and your pet soaked up the attention.",
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.INTERACTION
            )

            PetInteractionType.TAP -> MemoryCard(
                id = event.eventId,
                title = "A quick pet",
                summary = "You gave your pet a quick tap to say hello.",
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.INTERACTION
            )
        }
    }

    private fun mapActivity(event: EventEnvelope): MemoryCard? {
        val payload = PetActivityAppliedEventPayload.fromJson(event.payloadJson) ?: return null
        val activityType = payload.activityType.toPetActivityType() ?: return null
        return when (activityType) {
            PetActivityType.FEED -> MemoryCard(
                id = event.eventId,
                title = "Mealtime",
                summary = buildString {
                    append("Your pet ate and felt ")
                    append(payload.resultingMood.lowercase().replace('_', ' '))
                    append('.')
                },
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )

            PetActivityType.PLAY -> MemoryCard(
                id = event.eventId,
                title = "Playtime",
                summary = buildString {
                    append("You played together and your pet felt ")
                    append(payload.resultingMood.lowercase().replace('_', ' '))
                    append('.')
                },
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )

            PetActivityType.REST -> MemoryCard(
                id = event.eventId,
                title = "A rest break",
                summary = buildString {
                    append("Your pet settled down and felt ")
                    append(payload.resultingMood.lowercase().replace('_', ' '))
                    append('.')
                },
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )
        }
    }
}

private fun String.toPetActivityType(): PetActivityType? {
    return PetActivityType.entries.firstOrNull { it.name == this }
}
