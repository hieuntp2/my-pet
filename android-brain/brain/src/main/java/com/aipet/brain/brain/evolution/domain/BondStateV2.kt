package com.aipet.brain.brain.evolution.domain

/**
 * Multidimensional relationship bond state persisted across sessions.
 * Each dimension captures a different quality of the pet–user relationship.
 */
data class BondStateV2(
    val id: String = "singleton",
    /** 0..1: Emotional warmth the pet feels toward the user. */
    val affection: Float,
    /** 0..1: How safe and consistent the user feels. */
    val trust: Float,
    /** 0..1: How strongly the pet seeks the user. */
    val dependency: Float,
    /** 0..1: How resilient the relationship is against short-term fluctuations. */
    val stability: Float,
    val lastUpdatedAtMs: Long
) {
    /**
     * Derives a human-readable relationship label from the current dimensions.
     * Used for debug, analytics, and behavior rule selection.
     */
    fun label(): String = when {
        affection < 0.2f && trust < 0.2f -> "distant"
        affection < 0.4f && trust < 0.4f -> "warming_up"
        affection >= 0.7f && trust >= 0.6f && stability >= 0.6f -> "secure"
        affection >= 0.7f && dependency >= 0.7f && stability < 0.5f -> "clingy"
        affection >= 0.6f && trust >= 0.5f -> "attached"
        trust < 0.3f && affection >= 0.4f -> "unstable"
        affection >= 0.3f && trust >= 0.3f && stability >= 0.4f -> "recovering"
        else -> "warming_up"
    }

    companion object {
        val DEFAULT = BondStateV2(
            affection = 0.2f,
            trust = 0.2f,
            dependency = 0.1f,
            stability = 0.3f,
            lastUpdatedAtMs = 0L
        )
    }
}
