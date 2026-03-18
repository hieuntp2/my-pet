package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardCategory
import com.aipet.brain.brain.memory.MemoryCardImportance
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class InMemoryMemoryCardRepositoryTest {
    @Test
    fun save_andGetById_roundTripPersistsCard() = runTest {
        val repository = InMemoryMemoryCardRepository()
        val card = MemoryCard(
            id = "card-1",
            title = "Played together",
            summary = "The pet enjoyed a short play session.",
            timestampMs = 1_000L,
            sourceEventType = EventType.PET_PLAYED,
            category = MemoryCardCategory.CARE,
            importance = MemoryCardImportance.ROUTINE
        )

        val saved = repository.save(card)
        val loaded = repository.getById("card-1")

        assertEquals(saved, loaded)
    }

    @Test
    fun listRecent_returnsNewestCardsFirstAndRespectsLimit() = runTest {
        val repository = InMemoryMemoryCardRepository()
        repository.save(
            MemoryCard(
                id = "card-old",
                title = "Older memory",
                summary = "The pet noticed an older interaction.",
                timestampMs = 1_000L,
                sourceEventType = EventType.USER_INTERACTED_PET,
                category = MemoryCardCategory.INTERACTION
            )
        )
        repository.save(
            MemoryCard(
                id = "card-new",
                title = "Newer memory",
                summary = "The pet remembers the latest moment.",
                timestampMs = 2_000L,
                sourceEventType = EventType.PET_GREETED,
                category = MemoryCardCategory.GREETING,
                importance = MemoryCardImportance.NOTABLE,
                notableMomentLabel = "Warm welcome"
            )
        )

        val recent = repository.listRecent(limit = 1)

        assertEquals(1, recent.size)
        assertEquals("card-new", recent.first().id)
    }

    @Test
    fun getById_blankIdReturnsNull() = runTest {
        val repository = InMemoryMemoryCardRepository()

        val loaded = repository.getById("   ")

        assertNull(loaded)
    }

    @Test
    fun repository_canBeInstantiatedWithoutRuntimeErrors() {
        val repository = InMemoryMemoryCardRepository()

        assertNotNull(repository)
    }
}
