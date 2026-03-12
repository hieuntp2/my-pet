package com.aipet.brain.brain.memory

data class WorkingMemory(
    val currentPersonId: String? = null,
    val currentObjectId: String? = null,
    val lastStimulusTs: Long? = null
)
