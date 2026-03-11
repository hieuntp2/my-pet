package com.aipet.brain.memory.objects

data class ObjectRecord(
    val objectId: String,
    val name: String,
    val createdAtMs: Long,
    val lastSeenAtMs: Long?,
    val seenCount: Int
)
