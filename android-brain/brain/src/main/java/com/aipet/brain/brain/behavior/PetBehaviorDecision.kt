package com.aipet.brain.brain.behavior

data class PetBehaviorDecision<T>(
    val selectedBehavior: T,
    val selectedLabel: String,
    val candidates: List<PetBehaviorCandidate<T>>
)
