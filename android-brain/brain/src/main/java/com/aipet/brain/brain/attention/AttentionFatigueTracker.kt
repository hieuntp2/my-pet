package com.aipet.brain.brain.attention

import com.aipet.brain.brain.b2.domain.WorkingContext

/**
 * Tracks attention fatigue based on focus duration, shift frequency, and stimulation load.
 * High fatigue reduces responsiveness to weak repeated stimuli.
 */
class AttentionFatigueTracker {

    private var lastFatigueUpdate: Long = 0L
    private var recentShiftCount: Int = 0
    private var recentShiftWindowStartMs: Long = 0L

    /**
     * Compute the current fatigue level given the active [AttentionState] and context.
     *
     * @param current  Current attention state.
     * @param shifted  Whether a shift just occurred.
     * @param ctx      Working context for stimulation pressure.
     * @return         Updated fatigue value [0, 1].
     */
    fun computeFatigue(
        current: AttentionState,
        shifted: Boolean,
        ctx: WorkingContext
    ): Float {
        val nowMs = ctx.snapshotAtMs
        if (shifted) {
            recentShiftCount++
            if (nowMs - recentShiftWindowStartMs > SHIFT_WINDOW_MS) {
                recentShiftWindowStartMs = nowMs
                recentShiftCount = 1
            }
        }

        val holdMs = current.activeTarget.holdDurationMs(nowMs)

        // Fatigue builds with very long holds and many recent shifts
        val holdFatigue = (holdMs / MAX_HOLD_BEFORE_FATIGUE_MS.toFloat()).coerceAtMost(0.5f)
        val shiftFatigue = (recentShiftCount.toFloat() / MAX_SHIFTS_BEFORE_FATIGUE).coerceAtMost(0.4f)

        // Environmental pressure also contributes
        val pressureFatigue = ctx.perception.environmentContext.interactionPressure * 0.2f

        // Decay: fatigue recovers slowly during calm states
        val baseDecay = if (current.mode == AttentionMode.DOZING || current.mode == AttentionMode.IDLE_SCANNING)
            FATIGUE_RECOVERY_RATE * 2f
        else
            FATIGUE_RECOVERY_RATE

        val rawFatigue = holdFatigue + shiftFatigue + pressureFatigue
        val decayed = (current.fatigue - baseDecay).coerceAtLeast(0f)
        return (rawFatigue * 0.3f + decayed * 0.7f).coerceIn(0f, 1f)
    }

    companion object {
        private const val SHIFT_WINDOW_MS = 10_000L
        private const val MAX_SHIFTS_BEFORE_FATIGUE = 8f
        private const val MAX_HOLD_BEFORE_FATIGUE_MS = 30_000L
        private const val FATIGUE_RECOVERY_RATE = 0.005f
    }
}
