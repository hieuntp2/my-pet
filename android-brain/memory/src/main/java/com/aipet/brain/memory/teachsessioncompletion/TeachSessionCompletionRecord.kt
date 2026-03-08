package com.aipet.brain.memory.teachsessioncompletion

data class TeachSessionCompletionRecord(
    val teachSessionId: String,
    val isCompletedConfirmed: Boolean,
    val confirmedAtMs: Long?,
    val updatedAtMs: Long
)
