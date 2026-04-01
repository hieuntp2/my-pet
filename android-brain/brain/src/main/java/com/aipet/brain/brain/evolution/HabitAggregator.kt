package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import com.aipet.brain.brain.evolution.domain.UserHabitRepository
import java.util.Calendar

/**
 * Updates the UserHabitProfile from a batch of recent episodes.
 * Learns preferred time slots, session length, interaction style, and consistency.
 * Anti-overfitting: requires repeated evidence before fixing a habit as confident.
 */
class HabitAggregator(
    private val repository: UserHabitRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun updateFromEpisodes(episodes: List<MemoryEpisode>) {
        if (episodes.size < MIN_EPISODES_FOR_LEARNING) return
        val now = nowProvider()
        val current = repository.load()

        // Time slot counts
        val daypartCounts = mutableMapOf<String, Int>()
        episodes.forEach { ep ->
            val phase = DayPhase.fromHour(hourOf(ep.startTimeMs)).name
            daypartCounts[phase] = (daypartCounts[phase] ?: 0) + 1
        }
        val sortedDayparts = daypartCounts.entries.sortedByDescending { it.value }
        val strongestDaypart = sortedDayparts.firstOrNull()?.key ?: "NONE"
        val topSlots = sortedDayparts.take(2).map { it.key }
        val preferredTimeSlotsJson = topSlots.joinToString(",", "[", "]") { "\"$it\"" }

        // Average session length — blend with prior
        val avgLen = episodes.map { it.durationMs }.average().toLong()
        val blendedAvgLen = if (current.avgSessionLengthMs > 0L) {
            (current.avgSessionLengthMs * 0.6 + avgLen * 0.4).toLong()
        } else {
            avgLen
        }

        // Average sessions per day over the observed window
        val windowDays = windowDays(episodes)
        val avgPerDay = if (windowDays > 0) {
            val blended = if (current.avgSessionsPerDay > 0f) {
                current.avgSessionsPerDay * 0.6f + (episodes.size.toFloat() / windowDays) * 0.4f
            } else {
                episodes.size.toFloat() / windowDays
            }
            blended
        } else {
            current.avgSessionsPerDay
        }

        // Dominant interaction style from episode tags
        val styleCounts = episodes.groupingBy { it.userBehaviorTag }.eachCount()
        val primaryStyle = styleCounts.maxByOrNull { it.value }?.key ?: current.primaryInteractionStyle

        // Consistency: how many distinct days had sessions in the last 7 days
        val recentWindowMs = 7 * 24 * 3600_000L
        val recentEpisodes = episodes.filter { now - it.startTimeMs <= recentWindowMs }
        val activeDays = recentEpisodes.map { dayKey(it.startTimeMs) }.distinct().size
        val consistency = (activeDays / 7f).coerceIn(0f, 1f)
        val blendedConsistency = if (current.recentConsistencyScore > 0f) {
            (current.recentConsistencyScore * 0.5f + consistency * 0.5f).coerceIn(0f, 1f)
        } else consistency

        val updated = current.copy(
            preferredTimeSlotsJson = preferredTimeSlotsJson,
            avgSessionLengthMs = blendedAvgLen,
            avgSessionsPerDay = avgPerDay,
            primaryInteractionStyle = primaryStyle,
            recentConsistencyScore = blendedConsistency,
            strongestDaypart = strongestDaypart,
            lastUpdatedAtMs = now
        )
        repository.save(updated)
    }

    private fun hourOf(timeMs: Long): Int {
        val cal = Calendar.getInstance().also { it.timeInMillis = timeMs }
        return cal.get(Calendar.HOUR_OF_DAY)
    }

    private fun dayKey(timeMs: Long): String {
        val cal = Calendar.getInstance().also { it.timeInMillis = timeMs }
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }

    private fun windowDays(episodes: List<MemoryEpisode>): Float {
        if (episodes.isEmpty()) return 0f
        val minMs = episodes.minOf { it.startTimeMs }
        val maxMs = episodes.maxOf { it.startTimeMs }
        val spanMs = maxMs - minMs
        return (spanMs / (24 * 3600_000f)).coerceAtLeast(1f)
    }

    private companion object {
        const val MIN_EPISODES_FOR_LEARNING = 2
    }
}
