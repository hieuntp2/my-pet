package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.MemoryEpisode
import com.aipet.brain.brain.evolution.domain.SemanticMemoryFact
import com.aipet.brain.brain.evolution.domain.SemanticMemoryRepository
import java.util.UUID
import java.util.Calendar

/**
 * Infers stable semantic facts from a batch of recent episodes.
 *
 * Fact families supported in v1:
 * - habit.preferred_time_slots
 * - interaction.primary_style
 * - relationship.recent_consistency
 * - care.recent_quality_band
 */
class SemanticFactInferenceEngine(
    private val repository: SemanticMemoryRepository,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {

    suspend fun inferFromEpisodes(episodes: List<MemoryEpisode>) {
        if (episodes.isEmpty()) return
        val now = nowProvider()
        inferPreferredTimeSlots(episodes, now)
        inferPrimaryInteractionStyle(episodes, now)
        inferRecentConsistency(episodes, now)
        inferCareQualityBand(episodes, now)
    }

    private suspend fun inferPreferredTimeSlots(episodes: List<MemoryEpisode>, now: Long) {
        val daypartCounts = mutableMapOf<String, Int>()
        episodes.forEach { episode ->
            val daypart = resolveDaypart(episode.startTimeMs)
            daypartCounts[daypart] = (daypartCounts[daypart] ?: 0) + 1
        }
        val sortedSlots = daypartCounts.entries
            .sortedByDescending { it.value }
            .take(2)
            .map { it.key }
        val valueJson = sortedSlots.joinToString(",", "[", "]") { "\"$it\"" }
        val confidence = (episodes.size.toFloat() / 10f).coerceIn(0.1f, 0.95f)
        upsertFact(
            key = "habit.preferred_time_slots",
            valueJson = valueJson,
            confidence = confidence,
            episodeCount = episodes.size,
            now = now
        )
    }

    private suspend fun inferPrimaryInteractionStyle(episodes: List<MemoryEpisode>, now: Long) {
        // Count dominant interaction styles from episode tags
        val styleCounts = mutableMapOf<String, Int>()
        episodes.forEach { episode ->
            styleCounts[episode.userBehaviorTag] = (styleCounts[episode.userBehaviorTag] ?: 0) + 1
        }
        val primaryStyle = styleCounts.maxByOrNull { it.value }?.key ?: "GENERAL"
        val confidence = (episodes.size.toFloat() / 8f).coerceIn(0.1f, 0.9f)
        upsertFact(
            key = "interaction.primary_style",
            valueJson = "\"$primaryStyle\"",
            confidence = confidence,
            episodeCount = episodes.size,
            now = now
        )
    }

    private suspend fun inferRecentConsistency(episodes: List<MemoryEpisode>, now: Long) {
        if (episodes.size < 3) return
        // Measure how evenly spread the episodes are across the last 7 days
        val windowMs = 7 * 24 * 3600_000L
        val recentEpisodes = episodes.filter { now - it.startTimeMs <= windowMs }
        val daysWithSessions = recentEpisodes
            .map { dayIndex(it.startTimeMs) }
            .distinct()
            .size
        val consistency = (daysWithSessions / 7f).coerceIn(0f, 1f)
        val confidence = (recentEpisodes.size.toFloat() / 5f).coerceIn(0.1f, 0.9f)
        upsertFact(
            key = "relationship.recent_consistency",
            valueJson = "$consistency",
            confidence = confidence,
            episodeCount = recentEpisodes.size,
            now = now
        )
    }

    private suspend fun inferCareQualityBand(episodes: List<MemoryEpisode>, now: Long) {
        if (episodes.isEmpty()) return
        val avgCare = episodes.map { it.careScoreDelta }.average().toFloat()
        val band = when {
            avgCare > 10 -> "HIGH"
            avgCare > 0 -> "MODERATE"
            avgCare > -10 -> "LOW"
            else -> "POOR"
        }
        val confidence = (episodes.size.toFloat() / 6f).coerceIn(0.1f, 0.9f)
        upsertFact(
            key = "care.recent_quality_band",
            valueJson = "\"$band\"",
            confidence = confidence,
            episodeCount = episodes.size,
            now = now
        )
    }

    private suspend fun upsertFact(
        key: String,
        valueJson: String,
        confidence: Float,
        episodeCount: Int,
        now: Long
    ) {
        val existing = repository.getByKey(key)
        val updatedFact = if (existing != null) {
            // Blend old confidence with new evidence
            val blendedConfidence = (existing.confidence * 0.6f + confidence * 0.4f).coerceIn(0f, 1f)
            existing.copy(
                valueJson = valueJson,
                confidence = blendedConfidence,
                sourceEpisodeCount = existing.sourceEpisodeCount + episodeCount,
                lastConfirmedAtMs = now,
                lastUpdatedAtMs = now
            )
        } else {
            SemanticMemoryFact(
                id = UUID.randomUUID().toString(),
                key = key,
                valueJson = valueJson,
                confidence = confidence,
                sourceEpisodeCount = episodeCount,
                firstLearnedAtMs = now,
                lastConfirmedAtMs = now,
                lastUpdatedAtMs = now
            )
        }
        repository.upsert(updatedFact)
    }

    private fun resolveDaypart(timeMs: Long): String {
        val cal = Calendar.getInstance().also { it.timeInMillis = timeMs }
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "MORNING"
            in 12..17 -> "DAY"
            in 18..22 -> "EVENING"
            else -> "NIGHT"
        }
    }

    private fun dayIndex(timeMs: Long): Int {
        val cal = Calendar.getInstance().also { it.timeInMillis = timeMs }
        return cal.get(Calendar.DAY_OF_YEAR)
    }
}
