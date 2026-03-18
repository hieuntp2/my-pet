package com.aipet.brain.memory.daily

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
import com.aipet.brain.brain.events.UserInteractedPetEventPayload
import com.aipet.brain.brain.interaction.PetInteractionType
import com.aipet.brain.brain.pet.PetState
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class EventDrivenDailySummaryGenerator(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) : DailySummaryGenerator {
    override fun generateForDate(
        targetDate: LocalDate,
        persistedEvents: List<EventEnvelope>,
        petStateSnapshot: PetState?,
        continuityLabel: String?
    ): DailySummary? {
        val dayEvents = persistedEvents
            .filter { it.timestampMs.toLocalDate() == targetDate }
            .sortedBy { it.timestampMs }
        if (dayEvents.isEmpty()) {
            return null
        }

        val greetingCount = dayEvents.count { it.type == EventType.PET_GREETED }
        val interactionCount = dayEvents.count {
            it.type == EventType.USER_INTERACTED_PET || it.type == EventType.PET_LONG_PRESSED
        }
        val feedCount = dayEvents.count { it.type == EventType.PET_FED }
        val playCount = dayEvents.count { it.type == EventType.PET_PLAYED }
        val restCount = dayEvents.count { it.type == EventType.PET_RESTED }
        val activityCount = feedCount + playCount + restCount
        val supportedMomentCount = greetingCount + interactionCount + activityCount
        if (supportedMomentCount == 0) {
            return null
        }

        val stateSnapshotForDay = petStateSnapshot
            ?.takeIf { it.lastUpdatedAt.toLocalDate() == targetDate }
        val dominantMood = dominantMoodFor(dayEvents) ?: stateSnapshotForDay?.mood?.name
        val highlights = buildHighlights(
            continuityLabel = continuityLabel,
            greetingCount = greetingCount,
            interactionCount = interactionCount,
            feedCount = feedCount,
            playCount = playCount,
            restCount = restCount,
            dominantMood = dominantMood
        )
        val title = when {
            continuityLabel == "New day" -> "A new day together"
            supportedMomentCount == 1 -> "A small check-in"
            activityCount + interactionCount >= 4 -> "A lively day"
            activityCount >= 2 -> "A cared-for day"
            interactionCount >= 2 -> "A cozy day"
            greetingCount > 0 -> "A warm hello"
            else -> "A quiet pet day"
        }
        val summary = buildString {
            append(supportedMomentCount)
            append(if (supportedMomentCount == 1) " saved moment" else " saved moments")
            append(" today")
            if (highlights.isNotEmpty()) {
                append(": ")
                append(highlights.take(3).joinToString(separator = ", "))
            }
            append('.')
        }

        return DailySummary(
            date = targetDate,
            title = title,
            summary = summary,
            interactionCount = interactionCount,
            activityCount = activityCount,
            highlights = highlights,
            dominantMood = dominantMood,
            energySnapshot = stateSnapshotForDay?.energy,
            hungerSnapshot = stateSnapshotForDay?.hunger,
            sleepinessSnapshot = stateSnapshotForDay?.sleepiness,
            continuityLabel = continuityLabel
        )
    }

    override fun generateAll(
        persistedEvents: List<EventEnvelope>,
        petStateSnapshot: PetState?,
        currentDate: LocalDate?,
        continuityLabelForCurrentDay: String?
    ): List<DailySummary> {
        return persistedEvents
            .map { it.timestampMs.toLocalDate() }
            .distinct()
            .sortedDescending()
            .mapNotNull { date ->
                generateForDate(
                    targetDate = date,
                    persistedEvents = persistedEvents,
                    petStateSnapshot = petStateSnapshot,
                    continuityLabel = if (currentDate != null && date == currentDate) {
                        continuityLabelForCurrentDay
                    } else {
                        null
                    }
                )
            }
    }

    private fun dominantMoodFor(dayEvents: List<EventEnvelope>): String? {
        val moodCounts = linkedMapOf<String, Int>()
        dayEvents.forEach { event ->
            when (event.type) {
                EventType.PET_GREETED -> {
                    PetGreetedEventPayload.fromJson(event.payloadJson)?.emotion?.let { mood ->
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                }

                EventType.PET_FED,
                EventType.PET_PLAYED,
                EventType.PET_RESTED -> {
                    PetActivityAppliedEventPayload.fromJson(event.payloadJson)?.resultingMood?.let { mood ->
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                }

                EventType.USER_INTERACTED_PET,
                EventType.PET_LONG_PRESSED -> {
                    UserInteractedPetEventPayload.fromJson(event.payloadJson)?.resultingMood?.let { mood ->
                        moodCounts[mood] = moodCounts.getOrDefault(mood, 0) + 1
                    }
                }

                else -> Unit
            }
        }
        return moodCounts.maxByOrNull { it.value }?.key
    }

    private fun buildHighlights(
        continuityLabel: String?,
        greetingCount: Int,
        interactionCount: Int,
        feedCount: Int,
        playCount: Int,
        restCount: Int,
        dominantMood: String?
    ): List<String> {
        return buildList {
            if (continuityLabel != null) {
                add(continuityLabel)
            }
            if (feedCount > 0) {
                add("${countLabel(feedCount, "meal", "meals")}")
            }
            if (playCount > 0) {
                add("${countLabel(playCount, "play session", "play sessions")}")
            }
            if (restCount > 0) {
                add("${countLabel(restCount, "rest break", "rest breaks")}")
            }
            if (interactionCount > 0) {
                add("${countLabel(interactionCount, "petting moment", "petting moments")}")
            }
            if (greetingCount > 0) {
                add("${countLabel(greetingCount, "greeting", "greetings")}")
            }
            dominantMood?.let { mood ->
                add("Ended feeling ${mood.lowercase().replace('_', ' ')}")
            }
        }
    }

    private fun countLabel(count: Int, singular: String, plural: String): String {
        return if (count == 1) {
            "1 $singular"
        } else {
            "$count $plural"
        }
    }

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }
}
