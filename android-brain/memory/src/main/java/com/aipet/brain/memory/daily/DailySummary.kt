package com.aipet.brain.memory.daily

import java.time.LocalDate

data class DailySummary(
    val date: LocalDate,
    val title: String,
    val summary: String,
    val interactionCount: Int,
    val activityCount: Int,
    val highlights: List<String>,
    val dominantMood: String? = null,
    val energySnapshot: Int? = null,
    val hungerSnapshot: Int? = null,
    val sleepinessSnapshot: Int? = null,
    val continuityLabel: String? = null
) {
    init {
        require(title.isNotBlank()) { "DailySummary title cannot be blank." }
        require(summary.isNotBlank()) { "DailySummary summary cannot be blank." }
        require(interactionCount >= 0) { "interactionCount cannot be negative." }
        require(activityCount >= 0) { "activityCount cannot be negative." }
    }
}
