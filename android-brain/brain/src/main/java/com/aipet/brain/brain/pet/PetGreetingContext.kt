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
    val traits: PetTrait?
) {
    val isDistant: Boolean get() = conditions.contains(PetCondition.DISTANT)
    val isNeedy: Boolean get() = conditions.contains(PetCondition.NEEDY)
    val isSleepy: Boolean get() = conditions.contains(PetCondition.SLEEPY)
    val isHungry: Boolean get() = conditions.contains(PetCondition.HUNGRY)
    val isPlayful: Boolean get() = conditions.contains(PetCondition.PLAYFUL)
}
