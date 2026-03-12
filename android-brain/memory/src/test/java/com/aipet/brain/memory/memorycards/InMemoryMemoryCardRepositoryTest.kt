package com.aipet.brain.memory.memorycards

import com.aipet.brain.brain.memory.MemoryCard
import com.aipet.brain.brain.memory.MemoryCardType
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
            timestampMs = 1_000L,
            type = MemoryCardType.INTERACTION,
            personId = "person-1",
            objectId = "object-1",
            attributes = mapOf("source" to "unit_test")
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
                timestampMs = 1_000L,
                type = MemoryCardType.DETECTION
            )
        )
        repository.save(
            MemoryCard(
                id = "card-new",
                timestampMs = 2_000L,
                type = MemoryCardType.SUMMARY
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
