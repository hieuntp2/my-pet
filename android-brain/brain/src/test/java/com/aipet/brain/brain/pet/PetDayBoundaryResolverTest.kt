package com.aipet.brain.brain.pet

import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Test

class PetDayBoundaryResolverTest {
    private val resolver = PetDayBoundaryResolver(zoneId = ZoneOffset.UTC)

    @Test
    fun resolve_returnsSameDayWhenDatesMatch() {
        val snapshot = resolver.resolve(
            previousUpdatedAt = 1_000L,
            now = 60_000L
        )

        assertEquals(PetDayBoundaryType.SAME_DAY_RETURN, snapshot.type)
    }

    @Test
    fun resolve_returnsNewDayWhenDatesDiffer() {
        val snapshot = resolver.resolve(
            previousUpdatedAt = 1_000L,
            now = 86_500_000L
        )

        assertEquals(PetDayBoundaryType.NEW_DAY_RETURN, snapshot.type)
    }
}
