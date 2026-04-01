package com.aipet.brain.brain.evolution

import java.util.Calendar

/**
 * Resolves the current day phase from a timestamp.
 */
object DayPhaseResolver {
    fun resolve(timeMs: Long = System.currentTimeMillis()): DayPhase {
        val cal = Calendar.getInstance().also { it.timeInMillis = timeMs }
        return DayPhase.fromHour(cal.get(Calendar.HOUR_OF_DAY))
    }
}
