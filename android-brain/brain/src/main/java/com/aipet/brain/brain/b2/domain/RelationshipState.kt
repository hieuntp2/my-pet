package com.aipet.brain.brain.b2.domain

/**
 * Relationship context consumed by the behavior engine.
 * Captures how the pet currently feels about the user based on interaction history.
 */
data class RelationshipState(
    /** 0–1: general level of attachment/bond built over time. */
    val familiarity: Float = 0.5f,
    /** 0–1: how much the pet trusts the user (slow-changing). */
    val trust: Float = 0.5f,
    /** 0–1: warmth of recent interactions (decays with inactivity). */
    val recentWarmth: Float = 0f,
    /** 0–1: degree of recent neglect (increases with absence). */
    val recentNeglect: Float = 0f,
    /** 0–1: how much the pet expects the user to respond. */
    val responsivenessExpectation: Float = 0.5f,
    /** Raw bond score (0–100) sourced from PetState for convenience. */
    val bondScore: Int = 50,
    val updatedAtMs: Long = 0L
) {
    /** Overall warmth modifier: positive = pet is warmer, negative = colder. */
    fun warmthModifier(): Float = (recentWarmth - recentNeglect * 0.5f).coerceIn(-1f, 1f)

    /** Whether the user can be considered "familiar enough" for social lock. */
    fun isFamiliar(): Boolean = familiarity > 0.4f

    companion object {
        val DEFAULT = RelationshipState()
    }
}
