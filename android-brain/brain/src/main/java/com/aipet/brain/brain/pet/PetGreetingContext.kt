package com.aipet.brain.brain.pet

import com.aipet.brain.brain.personality.PetTrait

/**
 * Full reunion context fed into the greeting resolver.
 * Separates the inputs from the resolution logic for testability and debug visibility.
 */
data class PetGreetingContext(
    val absenceBucket: AbsenceBucket,
    val state: PetState,
    val conditions: Set<PetCondition>,
    val traits: PetTrait?,
    /**
     * Reunion type from the evolution system (e.g. "RECOVERY_RETURN", "LONG_ABSENCE").
     * When null, the resolver falls back to absence-bucket logic only.
     */
    val evolutionReunionType: String? = null,
    /**
     * Bond affection score [0..1] from the evolution system.
     * Biases greeting warmth when available.
     */
    val evolutionBondAffection: Float = 0f,
    /**
     * Bond trust score [0..1] from the evolution system.
     * Biases hesitation/openness when available.
     */
    val evolutionBondTrust: Float = 0f
) {
    val isDistant: Boolean get() = conditions.contains(PetCondition.DISTANT)
    val isNeedy: Boolean get() = conditions.contains(PetCondition.NEEDY)
    val isSleepy: Boolean get() = conditions.contains(PetCondition.SLEEPY)
    val isHungry: Boolean get() = conditions.contains(PetCondition.HUNGRY)
    val isPlayful: Boolean get() = conditions.contains(PetCondition.PLAYFUL)
}
