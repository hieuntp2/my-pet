package com.aipet.brain.brain.attention

/**
 * Debug state snapshot for the attention system, exposed to the debug panel.
 */
data class AttentionDebugState(
    val currentMode: AttentionMode,
    val activeTarget: FocusTarget,
    val intensity: Float,
    val stickiness: Float,
    val fatigue: Float,
    val holdDurationMs: Long,
    val availableForInterrupt: Boolean,
    val topCandidates: List<AttentionCandidateDebugEntry>,
    val lastShiftAtMs: Long,
    val lastShiftReason: String
)

/**
 * Debug entry for a single candidate focus target.
 */
data class AttentionCandidateDebugEntry(
    val type: FocusTargetType,
    val id: String?,
    val rawSalience: Float,
    val switchCostPenalty: Float,
    val effectiveSalience: Float,
    val blocked: Boolean,
    val blockReason: String? = null
)
