package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.BondStateV2
import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ReunionResolverTest {
    private val resolver = ReunionResolver()

    private val bond = BondStateV2.DEFAULT.copy(
        affection = 0.55f,
        trust = 0.6f,
        dependency = 0.5f,
        stability = 0.6f,
        lastUpdatedAtMs = 1L
    )

    private val consistentEveningHabit = UserHabitProfile.DEFAULT.copy(
        strongestDaypart = "EVENING",
        recentConsistencyScore = 0.8f,
        lastUpdatedAtMs = 1L
    )

    @Test
    fun `resolve returns quick return for short absence`() {
        val result = resolver.resolve(
            context(
                absenceMs = 10 * 60_000L,
                habitProfile = consistentEveningHabit,
                currentDaypart = "EVENING"
            )
        )

        assertEquals(ReunionType.QUICK_RETURN, result)
    }

    @Test
    fun `resolve returns missed expected when consistent habit window is missed`() {
        val result = resolver.resolve(
            context(
                absenceMs = 5 * 3600_000L,
                habitProfile = consistentEveningHabit,
                currentDaypart = "DAY"
            )
        )

        assertEquals(ReunionType.MISSED_EXPECTED_RETURN, result)
    }

    @Test
    fun `resolve returns recovery return after neglect streak and day long gap`() {
        val result = resolver.resolve(
            context(
                absenceMs = 24 * 3600_000L,
                neglectStreak = 2,
                careStreak = 0,
                habitProfile = consistentEveningHabit,
                currentDaypart = "EVENING"
            )
        )

        assertEquals(ReunionType.RECOVERY_RETURN, result)
    }

    @Test
    fun `resolve returns long absence when gap is very long without recovery condition`() {
        val result = resolver.resolve(
            context(
                absenceMs = 20 * 3600_000L,
                neglectStreak = 0,
                careStreak = 2,
                habitProfile = consistentEveningHabit,
                currentDaypart = "EVENING"
            )
        )

        assertEquals(ReunionType.LONG_ABSENCE, result)
    }

    private fun context(
        absenceMs: Long,
        neglectStreak: Int = 0,
        careStreak: Int = 1,
        habitProfile: UserHabitProfile = UserHabitProfile.DEFAULT,
        currentDaypart: String = "DAY"
    ): ReunionResolver.ReunionContext {
        return ReunionResolver.ReunionContext(
            absenceMs = absenceMs,
            bond = bond,
            habitProfile = habitProfile,
            neglectStreak = neglectStreak,
            careStreak = careStreak,
            lastExpectedWindowDaypart = habitProfile.strongestDaypart,
            currentDaypart = currentDaypart,
            nowMs = 1_000_000L
        )
    }
}
