package com.aipet.brain.brain.pet

/**
 * Result of applying a care action to the pet state.
 * Captures state before/after, the resulting emotion, and diminishing-return flag.
 */
data class CareActionResult(
    val actionType: CareActionType,
    val stateBefore: PetState,
    val stateAfter: PetState,
    val resultingEmotion: PetEmotion,
    val didDiminishingReturnsApply: Boolean,
    val isRepairAction: Boolean,
    val burstCount: Int
) {
    val energyDelta: Int get() = stateAfter.energy - stateBefore.energy
    val hungerDelta: Int get() = stateAfter.hunger - stateBefore.hunger
    val sleepinessDelta: Int get() = stateAfter.sleepiness - stateBefore.sleepiness
    val socialDelta: Int get() = stateAfter.social - stateBefore.social
    val comfortDelta: Int get() = stateAfter.comfort - stateBefore.comfort
    val stimulationDelta: Int get() = stateAfter.stimulation - stateBefore.stimulation
    val moodValenceDelta: Int get() = stateAfter.moodValence - stateBefore.moodValence
    val trustDelta: Int get() = stateAfter.trustScore - stateBefore.trustScore
    val bondDelta: Int get() = stateAfter.bond - stateBefore.bond
    val neglectStreakDelta: Int get() = stateAfter.neglectStreak - stateBefore.neglectStreak
}
