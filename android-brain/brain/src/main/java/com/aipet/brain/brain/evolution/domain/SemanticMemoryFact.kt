package com.aipet.brain.brain.evolution.domain

/**
 * A stable or semi-stable fact inferred from repeated interaction episodes.
 * Examples: preferred time slots, primary interaction style, recent care quality band.
 */
data class SemanticMemoryFact(
    val id: String,
    /** Dot-namespaced key, e.g. "habit.preferred_time_slots" */
    val key: String,
    /** JSON-encoded value for the fact. */
    val valueJson: String,
    /** 0..1 confidence that this fact is accurate. */
    val confidence: Float,
    val sourceEpisodeCount: Int,
    val firstLearnedAtMs: Long,
    val lastConfirmedAtMs: Long,
    val lastUpdatedAtMs: Long
)
