package com.aipet.brain.memory.daily

import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.pet.PetState
import java.time.LocalDate

interface DailySummaryGenerator {
    fun generateForDate(
        targetDate: LocalDate,
        persistedEvents: List<EventEnvelope>,
        petStateSnapshot: PetState? = null,
        continuityLabel: String? = null
    ): DailySummary?

    fun generateAll(
        persistedEvents: List<EventEnvelope>,
        petStateSnapshot: PetState? = null,
        currentDate: LocalDate? = null,
        continuityLabelForCurrentDay: String? = null
    ): List<DailySummary>
}
