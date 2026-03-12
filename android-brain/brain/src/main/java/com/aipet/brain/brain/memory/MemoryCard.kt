package com.aipet.brain.brain.memory

data class MemoryCard(
    val id: String,
    val timestampMs: Long,
    val type: MemoryCardType,
    val personId: String? = null,
    val objectId: String? = null,
    val attributes: Map<String, String> = emptyMap()
) {
    init {
        require(id.isNotBlank()) { "MemoryCard id cannot be blank." }
    }
}

enum class MemoryCardType {
    INTERACTION,
    DETECTION,
    SUMMARY,
    SYSTEM,
    CUSTOM
}
