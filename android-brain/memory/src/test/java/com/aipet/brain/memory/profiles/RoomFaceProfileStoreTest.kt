package com.aipet.brain.memory.profiles

import com.aipet.brain.memory.db.FaceProfileDao
import com.aipet.brain.memory.db.FaceProfileEmbeddingEntity
import com.aipet.brain.memory.db.FaceProfileEntity
import com.aipet.brain.memory.db.FaceProfileObservationLinkEntity
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomFaceProfileStoreTest {
    @Test
    fun createProfileCandidate_andGetProfile_returnsPersistedProfile() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 1_000L },
            idProvider = { "profile-1" }
        )

        val created = store.createProfileCandidate(
            label = "  Candidate A ",
            note = "  debug_note "
        )
        val loaded = store.getProfile("profile-1")

        assertEquals("profile-1", created.profileId)
        assertEquals(FaceProfileStatus.CANDIDATE, created.status)
        assertEquals("Candidate A", created.label)
        assertEquals("debug_note", created.note)
        assertNotNull(loaded)
        assertEquals(created, loaded)
    }

    @Test
    fun linkObservationToProfile_andListProfileObservations_returnsLinkedRecords() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 2_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")

        val linked = store.linkObservationToProfile(
            observationId = "obs-1",
            profileId = "profile-1"
        )
        val observations = store.listProfileObservations("profile-1")

        assertTrue(linked)
        assertEquals(1, observations.size)
        assertEquals("profile-1", observations.first().profileId)
        assertEquals("obs-1", observations.first().observationId)
        assertEquals(2_000L, observations.first().linkedAtMs)
    }

    @Test
    fun linkObservationToProfile_doesNotRequireKnownPersonLinkage() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 3_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")

        val linked = store.linkObservationToProfile(
            observationId = "obs-without-person-link",
            profileId = "profile-1"
        )

        assertTrue(linked)
    }

    @Test
    fun linkObservationToProfile_returnsFalseWhenProfileDoesNotExist() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 4_000L }
        )

        val linked = store.linkObservationToProfile(
            observationId = "obs-1",
            profileId = "missing-profile"
        )

        assertFalse(linked)
    }

    @Test
    fun addEmbeddingToProfile_andGetEmbedding_returnsPersistedEmbedding() = runTest {
        val ids = mutableListOf("profile-1", "embedding-1")
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 5_000L },
            idProvider = { ids.removeAt(0) }
        )
        store.createProfileCandidate(label = "Candidate A")

        val attached = store.addEmbeddingToProfile(
            profileId = "profile-1",
            values = listOf(0.1f, 0.2f, 0.3f),
            metadata = "debug_sample"
        )
        val loaded = store.getEmbedding("embedding-1")

        assertNotNull(attached)
        assertNotNull(loaded)
        assertEquals("embedding-1", attached?.embeddingId)
        assertEquals("embedding-1", loaded?.embeddingId)
        assertEquals("profile-1", loaded?.profileId)
        assertEquals(3, loaded?.vectorDim)
        assertEquals(listOf(0.1f, 0.2f, 0.3f), loaded?.values)
        assertEquals("debug_sample", loaded?.metadata)
    }

    @Test
    fun listProfileEmbeddings_supportsMultipleEmbeddingsForOneProfile() = runTest {
        val nowValues = mutableListOf(10_000L, 11_000L, 12_000L)
        val idValues = mutableListOf("profile-1", "embedding-1", "embedding-2")
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { nowValues.removeAt(0) },
            idProvider = { idValues.removeAt(0) }
        )
        store.createProfileCandidate(label = "Candidate A")

        store.addEmbeddingToProfile(
            profileId = "profile-1",
            values = listOf(0.11f, 0.22f),
            metadata = "sample_1"
        )
        store.addEmbeddingToProfile(
            profileId = "profile-1",
            values = listOf(0.33f, 0.44f),
            metadata = "sample_2"
        )

        val embeddings = store.listProfileEmbeddings("profile-1")

        assertEquals(2, embeddings.size)
        assertEquals("embedding-2", embeddings[0].embeddingId)
        assertEquals("embedding-1", embeddings[1].embeddingId)
    }

    @Test
    fun addEmbeddingToProfile_returnsNullWhenProfileDoesNotExist() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 6_000L },
            idProvider = { "embedding-1" }
        )

        val attached = store.addEmbeddingToProfile(
            profileId = "missing-profile",
            values = listOf(0.1f, 0.2f),
            metadata = "debug"
        )

        assertNull(attached)
    }

    @Test
    fun addEmbeddingToProfile_doesNotAutoLinkKnownPersonIdentity() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 7_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")

        val attached = store.addEmbeddingToProfile(
            profileId = "profile-1",
            values = listOf(0.9f, 0.8f, 0.7f),
            metadata = "no_person_id_link"
        )

        assertNotNull(attached)
        assertEquals("profile-1", attached?.profileId)
    }

    @Test
    fun linkProfileToPerson_andGetPersonForProfile_resolvesProfileManually() = runTest {
        val personStore = FakePersonStore()
        personStore.insert(
            PersonRecord(
                personId = "person-1",
                displayName = "Alice",
                nickname = null,
                isOwner = false,
                createdAtMs = 1_000L,
                updatedAtMs = 1_000L,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = personStore,
            nowProvider = { 8_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")

        assertNull(store.getPersonForProfile("profile-1"))

        val linked = store.linkProfileToPerson(profileId = "profile-1", personId = "person-1")
        val linkedPerson = store.getPersonForProfile("profile-1")

        assertTrue(linked)
        assertNotNull(linkedPerson)
        assertEquals("person-1", linkedPerson?.personId)
        assertEquals("Alice", linkedPerson?.displayName)
    }

    @Test
    fun listProfilesForPerson_returnsProfilesLinkedToThatPerson() = runTest {
        val personStore = FakePersonStore()
        personStore.insert(
            PersonRecord(
                personId = "person-1",
                displayName = "Alice",
                nickname = null,
                isOwner = false,
                createdAtMs = 1_000L,
                updatedAtMs = 1_000L,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        val idValues = mutableListOf("profile-1", "profile-2")
        val nowValues = mutableListOf(9_000L, 9_500L, 10_000L, 10_500L)
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = personStore,
            nowProvider = { nowValues.removeAt(0) },
            idProvider = { idValues.removeAt(0) }
        )
        store.createProfileCandidate(label = "Candidate A")
        store.createProfileCandidate(label = "Candidate B")

        store.linkProfileToPerson(profileId = "profile-1", personId = "person-1")
        store.linkProfileToPerson(profileId = "profile-2", personId = "person-1")

        val linkedProfiles = store.listProfilesForPerson("person-1")

        assertEquals(2, linkedProfiles.size)
        assertEquals("profile-2", linkedProfiles[0].profileId)
        assertEquals("profile-1", linkedProfiles[1].profileId)
    }

    @Test
    fun unlinkProfileFromPerson_clearsManualResolutionLink() = runTest {
        val personStore = FakePersonStore()
        personStore.insert(
            PersonRecord(
                personId = "person-1",
                displayName = "Alice",
                nickname = null,
                isOwner = false,
                createdAtMs = 1_000L,
                updatedAtMs = 1_000L,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = personStore,
            nowProvider = { 11_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")
        store.linkProfileToPerson(profileId = "profile-1", personId = "person-1")

        val unlinked = store.unlinkProfileFromPerson("profile-1")
        val linkedPerson = store.getPersonForProfile("profile-1")

        assertTrue(unlinked)
        assertNull(linkedPerson)
    }

    @Test
    fun linkProfileToPerson_returnsFalseWhenPersonDoesNotExist() = runTest {
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = FakePersonStore(),
            nowProvider = { 12_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")

        val linked = store.linkProfileToPerson(profileId = "profile-1", personId = "missing-person")

        assertFalse(linked)
    }

    @Test
    fun recordKnownPersonSeenFromLinkedProfile_updatesLinkedPersonSeenState() = runTest {
        val personStore = FakePersonStore()
        personStore.insert(
            PersonRecord(
                personId = "person-1",
                displayName = "Alice",
                nickname = null,
                isOwner = false,
                createdAtMs = 1_000L,
                updatedAtMs = 1_000L,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = personStore,
            nowProvider = { 13_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")
        store.linkProfileToPerson(profileId = "profile-1", personId = "person-1")
        store.linkObservationToProfile(observationId = "obs-1", profileId = "profile-1")

        val result = store.recordKnownPersonSeenFromLinkedProfile(
            profileId = "profile-1",
            observationId = "obs-1",
            seenAtMs = 12_500L
        )

        assertEquals(LinkedProfileSeenPropagationStatus.SUCCESS, result.status)
        assertTrue(result.propagated)
        assertEquals("person-1", result.person?.personId)
        assertEquals(1, result.person?.seenCount)
        assertEquals(12_500L, result.person?.lastSeenAtMs)
    }

    @Test
    fun recordKnownPersonSeenFromLinkedProfile_unresolvedProfileDoesNotUpdateKnownPerson() = runTest {
        val personStore = FakePersonStore()
        personStore.insert(
            PersonRecord(
                personId = "person-1",
                displayName = "Alice",
                nickname = null,
                isOwner = false,
                createdAtMs = 1_000L,
                updatedAtMs = 1_000L,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        val store = RoomFaceProfileStore(
            faceProfileDao = FakeFaceProfileDao(),
            personStore = personStore,
            nowProvider = { 14_000L },
            idProvider = { "profile-1" }
        )
        store.createProfileCandidate(label = "Candidate A")
        store.linkObservationToProfile(observationId = "obs-1", profileId = "profile-1")

        val result = store.recordKnownPersonSeenFromLinkedProfile(
            profileId = "profile-1",
            observationId = "obs-1",
            seenAtMs = 13_500L
        )

        assertEquals(LinkedProfileSeenPropagationStatus.PROFILE_UNRESOLVED, result.status)
        assertFalse(result.propagated)
        assertEquals(0, personStore.getById("person-1")?.seenCount)
        assertEquals(null, personStore.getById("person-1")?.lastSeenAtMs)
    }
}

private class FakeFaceProfileDao : FaceProfileDao {
    private val profilesById = linkedMapOf<String, FaceProfileEntity>()
    private val links = mutableListOf<FaceProfileObservationLinkEntity>()
    private val embeddingsById = linkedMapOf<String, FaceProfileEmbeddingEntity>()

    override suspend fun insertProfile(profile: FaceProfileEntity) {
        if (profilesById.containsKey(profile.profileId)) {
            throw IllegalStateException("Profile already exists: ${profile.profileId}")
        }
        profilesById[profile.profileId] = profile
    }

    override suspend fun getProfileById(profileId: String): FaceProfileEntity? {
        return profilesById[profileId]
    }

    override suspend fun listProfiles(): List<FaceProfileEntity> {
        return profilesById.values.sortedWith(
            compareByDescending<FaceProfileEntity> { it.updatedAtMs }
                .thenBy { it.profileId }
        )
    }

    override suspend fun profileExists(profileId: String): Boolean {
        return profilesById.containsKey(profileId)
    }

    override suspend fun setLinkedPerson(profileId: String, personId: String, updatedAtMs: Long): Int {
        val current = profilesById[profileId] ?: return 0
        profilesById[profileId] = current.copy(
            linkedPersonId = personId,
            updatedAtMs = updatedAtMs
        )
        return 1
    }

    override suspend fun clearLinkedPerson(profileId: String, updatedAtMs: Long): Int {
        val current = profilesById[profileId] ?: return 0
        if (current.linkedPersonId == null) {
            return 0
        }
        profilesById[profileId] = current.copy(
            linkedPersonId = null,
            updatedAtMs = updatedAtMs
        )
        return 1
    }

    override suspend fun listProfilesForPerson(personId: String): List<FaceProfileEntity> {
        return profilesById.values
            .asSequence()
            .filter { profile -> profile.linkedPersonId == personId }
            .sortedWith(
                compareByDescending<FaceProfileEntity> { profile -> profile.updatedAtMs }
                    .thenBy { profile -> profile.profileId }
            )
            .toList()
    }

    override suspend fun insertObservationLink(link: FaceProfileObservationLinkEntity): Long {
        val exists = links.any { existing ->
            existing.profileId == link.profileId && existing.observationId == link.observationId
        }
        if (exists) {
            return -1L
        }
        links.add(link)
        return 1L
    }

    override suspend fun listObservationLinks(profileId: String): List<FaceProfileObservationLinkEntity> {
        return links
            .asSequence()
            .filter { link -> link.profileId == profileId }
            .sortedWith(
                compareByDescending<FaceProfileObservationLinkEntity> { it.linkedAtMs }
                    .thenBy { it.observationId }
            )
            .toList()
    }

    override suspend fun hasObservationLink(profileId: String, observationId: String): Boolean {
        return links.any { link ->
            link.profileId == profileId && link.observationId == observationId
        }
    }

    override suspend fun insertEmbedding(embedding: FaceProfileEmbeddingEntity) {
        if (embeddingsById.containsKey(embedding.embeddingId)) {
            throw IllegalStateException("Embedding already exists: ${embedding.embeddingId}")
        }
        embeddingsById[embedding.embeddingId] = embedding
    }

    override suspend fun getEmbeddingById(embeddingId: String): FaceProfileEmbeddingEntity? {
        return embeddingsById[embeddingId]
    }

    override suspend fun listEmbeddingsByProfile(profileId: String): List<FaceProfileEmbeddingEntity> {
        return embeddingsById.values
            .asSequence()
            .filter { embedding -> embedding.profileId == profileId }
            .sortedWith(
                compareByDescending<FaceProfileEmbeddingEntity> { embedding -> embedding.createdAtMs }
                    .thenBy { embedding -> embedding.embeddingId }
            )
            .toList()
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
        val person = personsById[personId] ?: return false
        personsById.replaceAll { _, existing ->
            existing.copy(isOwner = existing.personId == person.personId)
        }
        return true
    }

    override suspend fun clearOwner(): Boolean {
        val hadOwner = personsById.values.any { person -> person.isOwner }
        if (!hadOwner) {
            return false
        }
        personsById.replaceAll { _, existing -> existing.copy(isOwner = false) }
        return true
    }

    override suspend fun recordPersonSeen(personId: String, seenAtMs: Long): PersonRecord? {
        val existing = personsById[personId] ?: return null
        val resolvedLastSeen = existing.lastSeenAtMs?.let { previous ->
            if (seenAtMs >= previous) seenAtMs else previous
        } ?: seenAtMs
        val updated = existing.copy(
            seenCount = existing.seenCount + 1,
            lastSeenAtMs = resolvedLastSeen,
            updatedAtMs = seenAtMs
        )
        personsById[personId] = updated
        return updated
    }
}
