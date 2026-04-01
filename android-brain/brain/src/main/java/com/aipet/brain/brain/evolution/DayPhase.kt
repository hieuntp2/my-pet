package com.aipet.brain.brain.evolution

/**
 * Observed day lifecycle phases.
 * Used for habit learning, expectation logic, and lifecycle baseline biases.
 */
enum class DayPhase {
    MORNING,  // 05:00–11:59
    DAY,      // 12:00–17:59
    EVENING,  // 18:00–22:59
    NIGHT;    // 23:00–04:59

    companion object {
        fun fromHour(hour: Int): DayPhase = when (hour) {
            in 5..11 -> MORNING
            in 12..17 -> DAY
            in 18..22 -> EVENING
            else -> NIGHT
        }
    }
}
