package com.aipet.brain.brain.memory

import com.aipet.brain.brain.events.EventType

data class MemoryCard(
    val id: String,
    val title: String,
    val summary: String,
    val timestampMs: Long,
    val sourceEventType: EventType,
    val category: MemoryCardCategory = MemoryCardCategory.INTERACTION,
    val importance: MemoryCardImportance = MemoryCardImportance.ROUTINE,
    val notableMomentLabel: String? = null
) {
    init {
        require(id.isNotBlank()) { "MemoryCard id cannot be blank." }
        require(title.isNotBlank()) { "MemoryCard title cannot be blank." }
        require(summary.isNotBlank()) { "MemoryCard summary cannot be blank." }
    }
}

enum class MemoryCardCategory {
    INTERACTION,
    CARE,
    GREETING
}

enum class MemoryCardImportance {
    ROUTINE,
    NOTABLE
}
