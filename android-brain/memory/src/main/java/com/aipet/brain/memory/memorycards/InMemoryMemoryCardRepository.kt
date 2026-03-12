package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardRepository
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class InMemoryMemoryCardRepository : MemoryCardRepository {
    private val lock = Mutex()
    private val cardsById = LinkedHashMap<String, MemoryCard>()

    override suspend fun save(card: MemoryCard): MemoryCard {
        val normalizedCard = card.normalized()
        lock.withLock {
            cardsById[normalizedCard.id] = normalizedCard
        }
        return normalizedCard
    }

    override suspend fun getById(id: String): MemoryCard? {
        val normalizedId = id.trim()
        if (normalizedId.isBlank()) {
            return null
        }
        return lock.withLock {
            cardsById[normalizedId]
        }
    }

    override suspend fun listRecent(limit: Int): List<MemoryCard> {
        if (limit <= 0) {
            return emptyList()
        }
        return lock.withLock {
            cardsById.values
                .sortedWith(
                    compareByDescending<MemoryCard> { card -> card.timestampMs }
                        .thenBy { card -> card.id }
                )
                .take(limit)
        }
    }

    private fun MemoryCard.normalized(): MemoryCard {
        val normalizedId = id.trim()
        require(normalizedId.isNotBlank()) { "MemoryCard id cannot be blank." }
        val normalizedPersonId = personId?.trim()?.ifBlank { null }
        val normalizedObjectId = objectId?.trim()?.ifBlank { null }
        val normalizedAttributes = attributes.entries
            .mapNotNull { entry ->
                val key = entry.key.trim()
                if (key.isBlank()) {
                    null
                } else {
                    key to entry.value
                }
            }
            .toMap()
        return copy(
            id = normalizedId,
            personId = normalizedPersonId,
            objectId = normalizedObjectId,
            attributes = normalizedAttributes
        )
    }
}
