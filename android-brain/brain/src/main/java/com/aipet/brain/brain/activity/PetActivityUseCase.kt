package com.aipet.brain.brain.activity

import com.aipet.brain.brain.pet.PetState

interface PetActivityUseCase {
    fun execute(
        currentState: PetState,
        actedAtMs: Long
    ): PetActivityResult
}
