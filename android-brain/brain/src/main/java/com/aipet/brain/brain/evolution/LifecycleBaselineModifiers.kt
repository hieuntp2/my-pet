package com.aipet.brain.brain.evolution

/**
 * Day-phase baseline biases applied to pet behavior selection.
 * These are additive modifiers — they bias, not override, the base system.
 *
 * @param energyBias         Positive = more energy than base; negative = less.
 * @param moodValenceBias    Positive = warmer valence; negative = cooler.
 * @param initiativeBias     0..1 modifier on invitation likelihood.
 * @param greetingWarmthMod  Multiplier on greeting style warmth selection (0.5–1.5).
 */
data class LifecycleBaselineModifiers(
    val energyBias: Int,
    val moodValenceBias: Int,
    val initiativeBias: Float,
    val greetingWarmthMod: Float
) {
    companion object {
        val MORNING = LifecycleBaselineModifiers(
            energyBias = +5,
            moodValenceBias = +5,
            initiativeBias = 0.6f,
            greetingWarmthMod = 1.1f
        )
        val DAY = LifecycleBaselineModifiers(
            energyBias = 0,
            moodValenceBias = 0,
            initiativeBias = 0.8f,
            greetingWarmthMod = 1.0f
        )
        val EVENING = LifecycleBaselineModifiers(
            energyBias = -5,
            moodValenceBias = +10,
            initiativeBias = 1.0f,
            greetingWarmthMod = 1.2f
        )
        val NIGHT = LifecycleBaselineModifiers(
            energyBias = -15,
            moodValenceBias = -5,
            initiativeBias = 0.3f,
            greetingWarmthMod = 0.8f
        )

        fun for_(phase: DayPhase): LifecycleBaselineModifiers = when (phase) {
            DayPhase.MORNING -> MORNING
            DayPhase.DAY -> DAY
            DayPhase.EVENING -> EVENING
            DayPhase.NIGHT -> NIGHT
        }
    }
}
