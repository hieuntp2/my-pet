package com.aipet.brain.brain.evolution.domain

/**
 * A grouped interaction session representing a meaningful interaction window.
 * Episodes move beyond raw event history to encode emotional meaning and care quality.
 */
data class MemoryEpisode(
    val id: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val durationMs: Long,
    val eventCount: Int,
    val interactionCount: Int,
    /** JSON-encoded list of interaction type strings. */
    val interactionTypesJson: String,
    val dominantPetEmotion: String,
    val dominantPetMood: String,
    val careScoreDelta: Int,
    val bondDelta: Int,
    val neglectSignal: Boolean,
    val reunionType: String,
    val userBehaviorTag: String,
    val importanceScore: Float,
    val summaryText: String,
    val createdAtMs: Long
)
