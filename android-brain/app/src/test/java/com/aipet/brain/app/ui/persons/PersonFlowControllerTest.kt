package com.aipet.brain.app.ui.persons

import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PersonFlowControllerTest {
    @Test
    fun loadPersons_returnsStoreData() = runTest {
        val fakeStore = FakePersonStore()
        val existing = testPerson(
            personId = "person-1",
            displayName = "Alex"
        )
        fakeStore.insert(existing)
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 1_000L },
            idProvider = { "generated-id" }
        )

        val persons = controller.loadPersons()

        assertEquals(listOf(existing), persons)
    }

    @Test
    fun createPerson_persistsValidatedFields() = runTest {
        val fakeStore = FakePersonStore()
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 5_000L },
            idProvider = { "person-created" }
        )

        val result = controller.createPerson(
            PersonEditorInput(
                displayName = "  Alice  ",
                nickname = "  Ally  "
            )
        )
        val saved = fakeStore.getById("person-created")

        assertEquals(PersonSaveResult.Success("person-created"), result)
        assertEquals("Alice", saved?.displayName)
        assertEquals("Ally", saved?.nickname)
        assertEquals(false, saved?.isOwner)
        assertEquals(5_000L, saved?.createdAtMs)
        assertEquals(5_000L, saved?.updatedAtMs)
        assertEquals(0, saved?.seenCount)
    }

    @Test
    fun updatePerson_updatesBasicFields() = runTest {
        val fakeStore = FakePersonStore()
        val existing = testPerson(
            personId = "person-1",
            displayName = "Before",
            nickname = "Nick",
            isOwner = false,
            createdAtMs = 1_000L,
            updatedAtMs = 1_500L
        )
        fakeStore.insert(existing)
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 3_000L },
            idProvider = { "unused" }
        )

        val result = controller.updatePerson(
            existingPerson = existing,
            input = PersonEditorInput(
                displayName = " After ",
                nickname = " "
            )
        )
        val updated = fakeStore.getById(existing.personId)

        assertEquals(PersonSaveResult.Success(existing.personId), result)
        assertEquals("After", updated?.displayName)
        assertEquals(null, updated?.nickname)
        assertEquals(false, updated?.isOwner)
        assertEquals(1_000L, updated?.createdAtMs)
        assertEquals(3_000L, updated?.updatedAtMs)
    }

    @Test
    fun createPerson_blankDisplayName_returnsValidationError() = runTest {
        val fakeStore = FakePersonStore()
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 1_000L },
            idProvider = { "person-created" }
        )

        val result = controller.createPerson(
            PersonEditorInput(
                displayName = "   ",
                nickname = "Name"
            )
        )

        assertEquals(
            PersonSaveResult.ValidationError("Display name cannot be blank."),
            result
        )
        assertTrue(fakeStore.listAll().isEmpty())
    }

    @Test
    fun assignOwner_reassignsFromOnePersonToAnother() = runTest {
        val fakeStore = FakePersonStore()
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 2_000L },
            idProvider = { "unused" }
        )
        val personA = testPerson(personId = "person-a", displayName = "A")
        val personB = testPerson(personId = "person-b", displayName = "B")
        fakeStore.insert(personA)
        fakeStore.insert(personB)

        val firstResult = controller.assignOwner(personA.personId)
        val secondResult = controller.assignOwner(personB.personId)

        assertEquals(
            OwnerAssignmentResult.Success("Owner assigned successfully."),
            firstResult
        )
        assertEquals(
            OwnerAssignmentResult.Success("Owner assigned successfully."),
            secondResult
        )
        assertEquals(personB.personId, controller.loadOwner()?.personId)
        assertEquals(false, fakeStore.getById(personA.personId)?.isOwner)
        assertEquals(true, fakeStore.getById(personB.personId)?.isOwner)
    }

    @Test
    fun recordPersonSeen_updatesSeenCountAndLastSeen() = runTest {
        val fakeStore = FakePersonStore()
        val person = testPerson(personId = "person-1", displayName = "Alex")
        fakeStore.insert(person)
        val controller = PersonFlowController(
            personStore = fakeStore,
            nowProvider = { 7_000L },
            idProvider = { "unused" }
        )

        val result = controller.recordPersonSeen(person.personId)

        assertTrue(result is PersonSeenRecordResult.Success)
        val updated = fakeStore.getById(person.personId)
        assertEquals(1, updated?.seenCount)
        assertEquals(7_000L, updated?.lastSeenAtMs)
    }
}

private class FakePersonStore : PersonStore {
    private val personsById = linkedMapOf<String, PersonRecord>()

    override suspend fun insert(person: PersonRecord) {
        personsById[person.personId] = person
    }

    override suspend fun update(person: PersonRecord): Boolean {
        if (!personsById.containsKey(person.personId)) {
            return false
        }
        personsById[person.personId] = person
        return true
    }

    override suspend fun getById(personId: String): PersonRecord? {
        return personsById[personId]
    }

    override suspend fun listAll(): List<PersonRecord> {
        return personsById.values.toList()
    }

    override suspend fun getOwner(): PersonRecord? {
        return personsById.values.firstOrNull { person -> person.isOwner }
    }

    override suspend fun assignOwner(personId: String): Boolean {
        if (!personsById.containsKey(personId)) {
            return false
        }
        personsById.replaceAll { _, person ->
            person.copy(isOwner = person.personId == personId)
        }
        return true
    }

    override suspend fun clearOwner(): Boolean {
        val hadOwner = personsById.values.any { person -> person.isOwner }
        personsById.replaceAll { _, person -> person.copy(isOwner = false) }
        return hadOwner
    }

    override suspend fun recordPersonSeen(personId: String, seenAtMs: Long): PersonRecord? {
        val existing = personsById[personId] ?: return null
        val updated = existing.copy(
            seenCount = existing.seenCount + 1,
            lastSeenAtMs = seenAtMs,
            updatedAtMs = seenAtMs
        )
        personsById[personId] = updated
        return updated
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
    seenCount: Int = 0
): PersonRecord {
    return PersonRecord(
        personId = personId,
        displayName = displayName,
        nickname = nickname,
        isOwner = isOwner,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        lastSeenAtMs = lastSeenAtMs,
        seenCount = seenCount
    )
}
