package com.aipet.brain.memory.persons

data class PersonRecord(
    val personId: String,
    val displayName: String,
    val nickname: String?,
    val isOwner: Boolean,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val lastSeenAtMs: Long?,
    val seenCount: Int,
    val familiarityScore: Float = 0f
)
