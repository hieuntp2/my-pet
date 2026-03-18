package com.aipet.brain.brain.behavior

import com.aipet.brain.brain.personality.PetTrait
import com.aipet.brain.brain.pet.PetCondition
import com.aipet.brain.brain.pet.PetState

data class PetBehaviorContext(
    val state: PetState,
    val conditions: Set<PetCondition>,
    val traits: PetTrait?
)
