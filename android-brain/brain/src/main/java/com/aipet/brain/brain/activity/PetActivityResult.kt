package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetState

data class PetActivityResult(
    val activityType: PetActivityType,
    val previousState: PetState,
    val updatedState: PetState,
    val delta: PetActivityStateDelta,
    val reason: String
)
