package com.aipet.brain.memory.daily

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PetActivityAppliedEventPayload
import com.aipet.brain.brain.events.PetGreetedEventPayload
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

        val stateSnapshotForDay = petStateSnapshot
            ?.takeIf { it.lastUpdatedAt.toLocalDate() == targetDate }
        val honestEnoughForSummary = supportedMomentCount >= 2 || activityCount > 0 ||
            stateSnapshotForDay != null
        if (!honestEnoughForSummary) {
            return null
        }

        val moodCounts = linkedMapOf<String, Int>()
        dayEvents.forEach { event ->
            when (event.type) {
                EventType.PET_GREETED -> {
                    val mood = PetGreetedEventPayload.fromJson(event.payloadJson)?.emotion
                    mood?.let { moodCounts[it] = moodCounts.getOrDefault(it, 0) + 1 }
                }

                EventType.PET_FED,
                EventType.PET_PLAYED,
                EventType.PET_RESTED -> {
                    val mood = PetActivityAppliedEventPayload.fromJson(event.payloadJson)?.resultingMood
                    mood?.let { moodCounts[it] = moodCounts.getOrDefault(it, 0) + 1 }
                }

                else -> Unit
            }
        }
        val dominantMood = moodCounts.maxByOrNull { it.value }?.key ?: stateSnapshotForDay?.mood?.name

        val highlights = buildList {
            if (continuityLabel != null) {
                add(continuityLabel)
            }
            if (feedCount > 0) {
                add("Fed $feedCount time${if (feedCount == 1) "" else "s"}")
            }
            if (playCount > 0) {
                add("Played $playCount time${if (playCount == 1) "" else "s"}")
            }
            if (restCount > 0) {
                add("Rested $restCount time${if (restCount == 1) "" else "s"}")
            }
            if (interactionCount > 0) {
                add("Shared $interactionCount petting moment${if (interactionCount == 1) "" else "s"}")
            }
            if (greetingCount > 0) {
                add("Exchanged $greetingCount greeting${if (greetingCount == 1) "" else "s"}")
            }
            dominantMood?.let { mood ->
                add("Ended feeling ${mood.lowercase().replace('_', ' ')}")
            }
        }

        val title = when {
            continuityLabel == "New day" -> "A new day together"
            activityCount >= 2 -> "A cared-for day"
            interactionCount >= 2 -> "A cozy day"
            greetingCount > 0 -> "A warm check-in"
            else -> "A quiet pet day"
        }
        val summary = buildString {
            append("$supportedMomentCount saved pet moment")
            append(if (supportedMomentCount == 1) "" else "s")
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

    private fun Long.toLocalDate(): LocalDate {
        return Instant.ofEpochMilli(this).atZone(zoneId).toLocalDate()
    }
}
