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
import java.time.LocalDate
import java.time.ZoneId
import kotlin.collections.ArrayDeque

class EventToMemoryCardMapper(
    private val zoneId: ZoneId = ZoneId.systemDefault(),
    private val longAbsenceThresholdMs: Long = 6 * 60 * 60 * 1000L,
    private val highActivityWindowMs: Long = 20 * 60 * 1000L,
    private val highActivityThreshold: Int = 4
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
        val mappedEvents = events
            .sortedBy { it.timestampMs }
            .mapNotNull { event ->
                map(event)?.let { card ->
                    MappedEventCard(event = event, card = card)
                }
            }
        if (mappedEvents.isEmpty()) {
            return emptyList()
        }

        val firstFeedDates = linkedSetOf<LocalDate>()
        val firstInteractionDates = linkedSetOf<LocalDate>()
        val notableLabelsById = linkedMapOf<String, String>()
        val activityWindow = ArrayDeque<Long>()
        var previousSupportedTimestampMs: Long? = null

        mappedEvents.forEach { mapped ->
            val event = mapped.event
            val card = mapped.card
            val cardDate = card.timestampMs.toLocalDate()

            when (event.type) {
                EventType.USER_INTERACTED_PET,
                EventType.PET_LONG_PRESSED -> {
                    if (firstInteractionDates.add(cardDate)) {
                        notableLabelsById.putIfAbsent(card.id, "First hello today")
                    }
                }

                EventType.PET_FED -> {
                    if (firstFeedDates.add(cardDate)) {
                        notableLabelsById.putIfAbsent(card.id, "First meal today")
                    }
                }

                else -> Unit
            }

            val previousTimestampMs = previousSupportedTimestampMs
            if (previousTimestampMs != null && card.timestampMs - previousTimestampMs >= longAbsenceThresholdMs) {
                notableLabelsById.putIfAbsent(card.id, "Back together")
            }

            if (event.type in HIGH_ACTIVITY_EVENT_TYPES) {
                while (activityWindow.isNotEmpty() && card.timestampMs - activityWindow.first() > highActivityWindowMs) {
                    activityWindow.removeFirst()
                }
                activityWindow.addLast(card.timestampMs)
                if (activityWindow.size >= highActivityThreshold) {
                    notableLabelsById.putIfAbsent(card.id, "Playful streak")
                }
            }

            extremeMomentLabelFor(event)?.let { label ->
                notableLabelsById.putIfAbsent(card.id, label)
            }

            previousSupportedTimestampMs = card.timestampMs
        }

        return mappedEvents
            .map { mapped ->
                val notableLabel = notableLabelsById[mapped.card.id]
                if (notableLabel == null) {
                    mapped.card
                } else {
                    mapped.card.copy(
                        importance = MemoryCardImportance.NOTABLE,
                        notableMomentLabel = notableLabel
                    )
                }
            }
            .sortedWith(compareByDescending<MemoryCard> { it.timestampMs }.thenBy { it.id })
    }

    private fun mapGreeting(event: EventEnvelope): MemoryCard? {
        val payload = PetGreetedEventPayload.fromJson(event.payloadJson) ?: return null
        return MemoryCard(
            id = event.eventId,
            title = greetingTitleFor(payload.emotion),
            summary = payload.message.asSentence(),
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
                title = "A close cuddle",
                summary = payload.feedbackText.asSentence(),
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.INTERACTION
            )

            PetInteractionType.TAP -> MemoryCard(
                id = event.eventId,
                title = "A quick hello",
                summary = payload.feedbackText.asSentence(),
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
                title = "Shared mealtime",
                summary = payload.feedbackText.asSentence(),
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )

            PetActivityType.PLAY -> MemoryCard(
                id = event.eventId,
                title = "Playtime together",
                summary = payload.feedbackText.asSentence(),
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )

            PetActivityType.REST -> MemoryCard(
                id = event.eventId,
                title = "A calm rest",
                summary = payload.feedbackText.asSentence(),
                timestampMs = event.timestampMs,
                sourceEventType = event.type,
                category = MemoryCardCategory.CARE
            )
        }
    }

    private fun extremeMomentLabelFor(event: EventEnvelope): String? {
        return when (event.type) {
            EventType.PET_GREETED -> {
                when (PetGreetedEventPayload.fromJson(event.payloadJson)?.emotion) {
                    "HUNGRY" -> "Needed care"
                    "SLEEPY" -> "Sleepy moment"
                    "EXCITED" -> "Big energy"
                    else -> null
                }
            }

            EventType.PET_PLAYED,
            EventType.PET_RESTED,
            EventType.PET_FED -> {
                val payload = PetActivityAppliedEventPayload.fromJson(event.payloadJson) ?: return null
                when {
                    payload.resultingMood == "EXCITED" -> "Big energy"
                    payload.resultingMood == "SLEEPY" -> "Sleepy moment"
                    payload.activityType == PetActivityType.FEED.name && payload.hungerDelta <= -20 -> "Needed care"
                    else -> null
                }
            }

            else -> null
        }
    }

    private fun greetingTitleFor(emotion: String): String {
        return when (emotion) {
            "HUNGRY" -> "A hungry hello"
            "SLEEPY" -> "A sleepy hello"
            "CURIOUS" -> "A curious hello"
            "EXCITED", "HAPPY" -> "A warm hello"
            else -> "A gentle hello"
        }
    }

    private fun String.asSentence(): String {
        val trimmed = trim()
        if (trimmed.isEmpty()) {
            return this
        }
        return if (trimmed.endsWith('.') || trimmed.endsWith('!') || trimmed.endsWith('?')) {
            trimmed
        } else {
            "$trimmed."
        }
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }

    private data class MappedEventCard(
        val event: EventEnvelope,
        val card: MemoryCard
    )

    private companion object {
        val HIGH_ACTIVITY_EVENT_TYPES = setOf(
            EventType.USER_INTERACTED_PET,
            EventType.PET_LONG_PRESSED,
            EventType.PET_FED,
            EventType.PET_PLAYED,
            EventType.PET_RESTED
        )
    }
}

private fun String.toPetActivityType(): PetActivityType? {
    return PetActivityType.entries.firstOrNull { it.name == this }
}
