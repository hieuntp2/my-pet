package com.aipet.brain.brain.pet

/**
 * Detects when the pet is overstimulated from rapid interaction bursts and
 * applies the appropriate state cost. Prevents spam-tapping from being the
 * optimal interaction strategy.
 */
class OverstimulationTracker {

    /**
     * Evaluates whether the current burst count and stimulation level trigger
     * overstimulation and applies its cost.
     */
    fun applyIfNeeded(state: PetState, burstCount: Int, now: Long): PetState {
        if (state.stimulation < PetEmotionalConfig.OVERSTIM_STIMULATION_THRESHOLD) {
            return state
        }
        if (burstCount <= PetEmotionalConfig.BURST_COUNT_DIMINISH_START) {
            return state
        }

        return state.copy(
            comfort = (state.comfort - PetEmotionalConfig.OVERSTIM_COMFORT_DROP).coerceAtLeast(PetState.VALUE_MIN),
            trustScore = (state.trustScore - PetEmotionalConfig.OVERSTIM_TRUST_DROP).coerceAtLeast(PetState.VALUE_MIN)
        ).withClampedValues(lastUpdatedAt = now)
    }

    fun isOverstimulated(state: PetState): Boolean {
        return state.stimulation >= PetEmotionalConfig.OVERSTIM_STIMULATION_THRESHOLD
    }
}
