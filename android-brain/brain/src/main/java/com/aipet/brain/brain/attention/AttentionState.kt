package com.aipet.brain.brain.attention

/**
 * The full attention state at a given moment.
 *
 * @param activeTarget          What the pet currently attends to (null = nothing specific).
 * @param mode                  How the pet is attending.
 * @param intensity             0–1: how focused/engaged the attention is.
 * @param stickiness            0–1: resistance to switching to a new target (inertia).
 * @param fatigue               0–1: attention fatigue; reduces responsiveness to weak stimuli.
 * @param availableForInterrupt Whether a high-urgency event can break current focus.
 * @param lastShiftAtMs         When the target last changed.
 */
data class AttentionState(
    val activeTarget: FocusTarget = FocusTarget.NONE,
    val mode: AttentionMode = AttentionMode.IDLE_SCANNING,
    val intensity: Float = 0f,
    val stickiness: Float = 0.3f,
    val fatigue: Float = 0f,
    val availableForInterrupt: Boolean = true,
    val lastShiftAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    companion object {
        val DEFAULT = AttentionState()
    }
}
