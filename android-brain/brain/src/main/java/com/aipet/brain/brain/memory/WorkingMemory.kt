package com.aipet.brain.brain.memory

data class WorkingMemory(
    val currentPersonId: String? = null,
    val currentObjectLabel: String? = null,
    val lastStimulusAtMs: Long? = null
)
