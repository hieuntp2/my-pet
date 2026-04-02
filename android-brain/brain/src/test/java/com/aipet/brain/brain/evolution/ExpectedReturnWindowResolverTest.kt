package com.aipet.brain.brain.evolution

import com.aipet.brain.brain.evolution.domain.UserHabitProfile
import org.junit.Assert.assertEquals
import org.junit.Test

class ExpectedReturnWindowResolverTest {
    private val resolver = ExpectedReturnWindowResolver()

    private val consistentEveningProfile = UserHabitProfile.DEFAULT.copy(
        strongestDaypart = "EVENING",
        recentConsistencyScore = 0.8f,
        lastUpdatedAtMs = 1L
    )

    @Test
    fun `resolve returns early for very short absence`() {
        val result = resolver.resolve(
            profile = consistentEveningProfile,
            currentDayPhase = DayPhase.EVENING,
            absenceMs = 10 * 60_000L
        )

        assertEquals(ExpectedReturnWindowResolver.ExpectationState.EARLY, result)
    }

    @Test
    fun `resolve returns on time when current day phase matches strongest daypart`() {
        val result = resolver.resolve(
            profile = consistentEveningProfile,
            currentDayPhase = DayPhase.EVENING,
            absenceMs = 2 * 3600_000L
        )

        assertEquals(ExpectedReturnWindowResolver.ExpectationState.ON_TIME, result)
    }

    @Test
    fun `resolve returns slightly late when user arrives adjacent phase`() {
        val result = resolver.resolve(
            profile = consistentEveningProfile,
            currentDayPhase = DayPhase.NIGHT,
            absenceMs = 3 * 3600_000L
        )

        assertEquals(ExpectedReturnWindowResolver.ExpectationState.SLIGHTLY_LATE, result)
    }

    @Test
    fun `resolve returns missed window when absence is very long`() {
        val result = resolver.resolve(
            profile = consistentEveningProfile,
            currentDayPhase = DayPhase.MORNING,
            absenceMs = 9 * 3600_000L
        )

        assertEquals(ExpectedReturnWindowResolver.ExpectationState.MISSED_WINDOW, result)
    }
}
