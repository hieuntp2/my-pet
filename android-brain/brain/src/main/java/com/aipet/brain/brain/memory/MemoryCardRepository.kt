package com.aipet.brain.brain.memory

interface MemoryCardRepository {
    suspend fun save(card: MemoryCard): MemoryCard

    suspend fun getById(id: String): MemoryCard?

    suspend fun listRecent(limit: Int): List<MemoryCard>
}
