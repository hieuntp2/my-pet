package com.aipet.brain.app.ui.home

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardCategory
import com.aipet.brain.brain.memory.MemoryCardImportance
import com.aipet.brain.memory.daily.DailySummary
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTodaySummaryResolverTest {
    private val resolver = HomeTodaySummaryResolver(zoneId = ZoneOffset.UTC)

    @Test
    fun `resolve prefers daily summary for current day`() {
        val summary = resolver.resolve(
            memoryCards = emptyList(),
            dailySummaries = listOf(
                DailySummary(
                    date = LocalDate.of(2026, 3, 18),
                    title = "A cared-for day",
                    summary = "3 saved moments today: 1 meal, 1 play session, Ended feeling happy.",
                    interactionCount = 0,
                    activityCount = 2,
                    highlights = listOf("1 meal", "1 play session")
                )
            ),
            targetDate = LocalDate.of(2026, 3, 18)
        )

        assertEquals("Today with your pet", summary?.title)
    }

    @Test
    fun `resolve falls back to notable memory when daily summary is missing`() {
        val summary = resolver.resolve(
            memoryCards = listOf(
                MemoryCard(
                    id = "m1",
                    title = "Playtime together",
                    summary = "Your pet had fun playing with you.",
                    timestampMs = 1_000L,
                    sourceEventType = EventType.PET_PLAYED,
                    category = MemoryCardCategory.CARE,
                    importance = MemoryCardImportance.NOTABLE,
                    notableMomentLabel = "Playful streak"
                )
            ),
            dailySummaries = emptyList(),
            targetDate = LocalDate.of(1970, 1, 1)
        )

        assertEquals("Playful streak", summary?.title)
    }
}
