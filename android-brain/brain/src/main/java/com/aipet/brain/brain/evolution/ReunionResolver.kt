package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.UserHabitProfile

/**
 * Resolves the reunion type based on absence duration, bond state, habit, and neglect history.
 * Produces a [ReunionType] that drives greeting style and behavioral warmth.
 */
class ReunionResolver {

    data class ReunionContext(
        val absenceMs: Long,
        val bond: BondStateV2,
        val habitProfile: UserHabitProfile,
        val neglectStreak: Int,
        val careStreak: Int,
        val lastExpectedWindowDaypart: String,
        val currentDaypart: String,
        val nowMs: Long
    )

    fun resolve(context: ReunionContext): ReunionType {
        val absenceMs = context.absenceMs
        // Quick return — barely noticed
        if (absenceMs < 30 * 60_000L) return ReunionType.QUICK_RETURN

        // Coming back after a neglect streak — recovery mode
        if (context.neglectStreak >= 2 && absentForDays(absenceMs) >= 1) {
            return ReunionType.RECOVERY_RETURN
        }

        // Multi-day absence (more than 18 hours)
        if (absenceMs > 18 * 3600_000L) return ReunionType.LONG_ABSENCE

        // Check if user missed their expected window
        val strongestDaypart = context.habitProfile.strongestDaypart
        val consistency = context.habitProfile.recentConsistencyScore
        if (consistency >= MIN_CONSISTENCY_FOR_EXPECTATION &&
            strongestDaypart != "NONE" &&
            strongestDaypart != context.currentDaypart &&
            absenceMs > 4 * 3600_000L
        ) {
            return ReunionType.MISSED_EXPECTED_RETURN
        }

        // Returning within the expected pattern
        if (consistency >= MIN_CONSISTENCY_FOR_EXPECTATION &&
            strongestDaypart == context.currentDaypart
        ) {
            return ReunionType.ROUTINE_RETURN
        }

        // Default for gaps > 30 min but within the day
        return if (absenceMs > 4 * 3600_000L) ReunionType.LATE_RETURN
        else ReunionType.ROUTINE_RETURN
    }

    private fun absentForDays(absenceMs: Long): Int =
        (absenceMs / (24 * 3600_000L)).toInt()

    private companion object {
        const val MIN_CONSISTENCY_FOR_EXPECTATION = 0.35f
    }
}
