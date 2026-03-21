package com.aipet.brain.memory.persons

import com.aipet.brain.memory.db.PersonDao
import com.aipet.brain.memory.db.PersonEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomPersonStoreTest {
    @Test
    fun insert_andGetById_returnsPersistedPerson() = runTest {
        val store = RoomPersonStore(FakePersonDao())
        val person = testPerson(
            personId = "person-1",
            displayName = "Alex",
            nickname = "Al",
            isOwner = false
        )

        store.insert(person)
        val loaded = store.getById(person.personId)

        assertNotNull(loaded)
        assertEquals(person, loaded)
    }

    @Test
    fun listAll_returnsPersonsOrderedByFamiliarityDesc() = runTest {
        val store = RoomPersonStore(FakePersonDao())
        val lowerFamiliarity = testPerson(
            personId = "person-lower",
            displayName = "Lower",
            updatedAtMs = 2_000L,
            familiarityScore = 0.3f
        )
        val higherFamiliarity = testPerson(
            personId = "person-higher",
            displayName = "Higher",
            updatedAtMs = 1_000L,
            familiarityScore = 0.8f
        )

        store.insert(lowerFamiliarity)
        store.insert(higherFamiliarity)
        val allPersons = store.listAll()

        assertEquals(listOf("person-higher", "person-lower"), allPersons.map { it.personId })
    }

    @Test
    fun update_changesBasicFieldsAndReturnsTrue() = runTest {
        val store = RoomPersonStore(FakePersonDao())
        val original = testPerson(
            personId = "person-1",
            displayName = "Alex",
            nickname = "Al",
            updatedAtMs = 1_000L
        )
        store.insert(original)
        val updated = original.copy(
            displayName = "Alexander",
            nickname = "Lex",
            updatedAtMs = 2_000L,
            lastSeenAtMs = 1_500L
        )

        val updatedResult = store.update(updated)
        val loaded = store.getById(original.personId)

        assertTrue(updatedResult)
        assertEquals(updated, loaded)
    }

    @Test
    fun update_returnsFalseWhenPersonDoesNotExist() = runTest {
        val store = RoomPersonStore(FakePersonDao())

        val updatedResult = store.update(
            testPerson(
                personId = "missing",
                displayName = "Missing"
            )
        )

        assertFalse(updatedResult)
    }

    @Test
    fun assignOwner_reassignsToOneOwnerOnly() = runTest {
        val store = RoomPersonStore(FakePersonDao(), nowProvider = { 3_000L })
        val first = testPerson(personId = "person-1", displayName = "First")
        val second = testPerson(personId = "person-2", displayName = "Second")
        store.insert(first)
        store.insert(second)

        val firstAssigned = store.assignOwner(first.personId)
        val secondAssigned = store.assignOwner(second.personId)

        assertTrue(firstAssigned)
        assertTrue(secondAssigned)
        assertEquals(second.personId, store.getOwner()?.personId)
        assertEquals(false, store.getById(first.personId)?.isOwner)
        assertEquals(true, store.getById(second.personId)?.isOwner)
    }

    @Test
    fun clearOwner_removesCurrentOwner() = runTest {
        val store = RoomPersonStore(FakePersonDao(), nowProvider = { 4_000L })
        val person = testPerson(personId = "person-1", displayName = "Owner Candidate")
        store.insert(person)
        store.assignOwner(person.personId)

        val cleared = store.clearOwner()

        assertTrue(cleared)
        assertEquals(null, store.getOwner())
    }

    @Test
    fun recordPersonSeen_incrementsSeenCountAndUpdatesLastSeen() = runTest {
        val store = RoomPersonStore(FakePersonDao(), nowProvider = { 7_000L })
        val person = testPerson(
            personId = "person-1",
            displayName = "Seen Person",
            lastSeenAtMs = 2_000L,
            seenCount = 1
        )
        store.insert(person)

        val updated = store.recordPersonSeen(
            personId = person.personId,
            seenAtMs = 6_000L
        )

        assertNotNull(updated)
        assertEquals(2, updated?.seenCount)
        assertEquals(6_000L, updated?.lastSeenAtMs)
        assertEquals(7_000L, updated?.updatedAtMs)
    }

    @Test
    fun recordPersonSeen_returnsNullWhenPersonDoesNotExist() = runTest {
        val store = RoomPersonStore(FakePersonDao(), nowProvider = { 8_000L })

        val updated = store.recordPersonSeen(
            personId = "missing-person",
            seenAtMs = 6_000L
        )

        assertEquals(null, updated)
    }

    @Test
    fun increaseFamiliarity_updatesScoreAndClampsToOne() = runTest {
        val store = RoomPersonStore(FakePersonDao())
        val person = testPerson(
            personId = "person-1",
            displayName = "Alex",
            familiarityScore = 0.98f
        )
        store.insert(person)

        val updated = store.increaseFamiliarity(
            personId = person.personId,
            delta = 0.1f,
            updatedAtMs = 9_000L
        )

        assertNotNull(updated)
        assertEquals(1.0f, updated?.familiarityScore)
        assertEquals(9_000L, updated?.updatedAtMs)
    }
}

private class FakePersonDao : PersonDao {
    private val personsById = linkedMapOf<String, PersonEntity>()

    override suspend fun insert(person: PersonEntity) {
        if (personsById.containsKey(person.personId)) {
            throw IllegalStateException("Person already exists: ${person.personId}")
        }
        personsById[person.personId] = person
    }

    override suspend fun update(person: PersonEntity): Int {
        if (!personsById.containsKey(person.personId)) {
            return 0
        }
        personsById[person.personId] = person
        return 1
    }

    override suspend fun getById(personId: String): PersonEntity? {
        return personsById[personId]
    }

    override suspend fun listAll(): List<PersonEntity> {
        return personsById.values.sortedWith(
            compareByDescending<PersonEntity> { it.familiarityScore }
                .thenByDescending { it.updatedAtMs }
                .thenBy { it.personId }
        )
    }

    override suspend fun listTopByFamiliarity(limit: Int): List<PersonEntity> {
        return listAll().take(limit.coerceAtLeast(0))
    }

    override suspend fun getOwner(): PersonEntity? {
        return personsById.values
            .filter { it.isOwner }
            .maxByOrNull { it.updatedAtMs }
    }

    override suspend fun clearOwnerFlags(updatedAtMs: Long): Int {
        val ownerIds = personsById.values
            .filter { entity -> entity.isOwner }
            .map { entity -> entity.personId }
        ownerIds.forEach { ownerId ->
            val existing = personsById[ownerId] ?: return@forEach
            personsById[ownerId] = existing.copy(
                isOwner = false,
                updatedAtMs = updatedAtMs
            )
        }
        return ownerIds.size
    }

    override suspend fun setOwnerById(personId: String, updatedAtMs: Long): Int {
        val existing = personsById[personId] ?: return 0
        personsById[personId] = existing.copy(
            isOwner = true,
            updatedAtMs = updatedAtMs
        )
        return 1
    }

    override suspend fun incrementSeenCount(
        personId: String,
        seenAtMs: Long,
        updatedAtMs: Long
    ): Int {
        val existing = personsById[personId] ?: return 0
        val lastSeenAtMs = existing.lastSeenAtMs
        val resolvedLastSeen = if (lastSeenAtMs == null || seenAtMs >= lastSeenAtMs) {
            seenAtMs
        } else {
            lastSeenAtMs
        }
        personsById[personId] = existing.copy(
            seenCount = existing.seenCount + 1,
            lastSeenAtMs = resolvedLastSeen,
            updatedAtMs = updatedAtMs
        )
        return 1
    }

    override suspend fun incrementFamiliarity(
        personId: String,
        delta: Float,
        updatedAtMs: Long
    ): Int {
        val existing = personsById[personId] ?: return 0
        personsById[personId] = existing.copy(
            familiarityScore = (existing.familiarityScore + delta).coerceIn(0f, 1f),
            updatedAtMs = updatedAtMs
        )
        return 1
    }

    override suspend fun deleteFaceProfilesByPersonId(personId: String): Int = 0

    override suspend fun deleteById(personId: String): Int {
        return if (personsById.remove(personId) != null) 1 else 0
    }
}

private fun testPerson(
    personId: String,
    displayName: String,
    nickname: String? = null,
    isOwner: Boolean = false,
    createdAtMs: Long = 1_000L,
    updatedAtMs: Long = 1_000L,
    lastSeenAtMs: Long? = null,
    seenCount: Int = 0,
    familiarityScore: Float = 0f
): PersonRecord {
    return PersonRecord(
        personId = personId,
        displayName = displayName,
        nickname = nickname,
        isOwner = isOwner,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        lastSeenAtMs = lastSeenAtMs,
        seenCount = seenCount,
        familiarityScore = familiarityScore
    )
}
