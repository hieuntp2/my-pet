package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardImportance
import com.aipet.brain.memory.daily.DailySummary
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class HomeTodaySummary(
    val title: String,
    val body: String
)

internal class HomeTodaySummaryResolver(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun resolve(
        memoryCards: List<MemoryCard>,
        dailySummaries: List<DailySummary>,
        targetDate: LocalDate
    ): HomeTodaySummary? {
        val summaryForToday = dailySummaries.firstOrNull { it.date == targetDate }
        if (summaryForToday != null) {
            return HomeTodaySummary(
                title = "Today with your pet",
                body = summaryForToday.summary
            )
        }

        val cardsForToday = memoryCards
            .filter { Instant.ofEpochMilli(it.timestampMs).atZone(zoneId).toLocalDate() == targetDate }
            .sortedByDescending { it.timestampMs }
        val notableCard = cardsForToday.firstOrNull { it.importance == MemoryCardImportance.NOTABLE }
        if (notableCard != null) {
            return HomeTodaySummary(
                title = notableCard.notableMomentLabel ?: "Today with your pet",
                body = notableCard.summary
            )
        }
        val recentCard = cardsForToday.firstOrNull() ?: return null
        return HomeTodaySummary(
            title = "Today with your pet",
            body = recentCard.summary
        )
    }
}
