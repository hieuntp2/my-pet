package com.aipet.brain.brain.pet

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

enum class PetDayBoundaryType {
    SAME_DAY_RETURN,
    NEW_DAY_RETURN
}

data class PetDayBoundarySnapshot(
    val type: PetDayBoundaryType,
    val previousDate: LocalDate,
    val currentDate: LocalDate
)

class PetDayBoundaryResolver(
    private val zoneId: ZoneId = ZoneId.systemDefault()
) {
    fun resolve(previousUpdatedAt: Long, now: Long): PetDayBoundarySnapshot {
        val previousDate = Instant.ofEpochMilli(previousUpdatedAt).atZone(zoneId).toLocalDate()
        val currentDate = Instant.ofEpochMilli(now).atZone(zoneId).toLocalDate()
        return PetDayBoundarySnapshot(
            type = if (previousDate == currentDate) {
                PetDayBoundaryType.SAME_DAY_RETURN
            } else {
                PetDayBoundaryType.NEW_DAY_RETURN
            },
            previousDate = previousDate,
            currentDate = currentDate
        )
    }
}
