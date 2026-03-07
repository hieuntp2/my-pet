package com.aipet.brain.memory.profiles

import com.aipet.brain.memory.db.FaceProfileDao
import com.aipet.brain.memory.db.FaceProfileEmbeddingEntity
import com.aipet.brain.memory.db.FaceProfileEntity
import com.aipet.brain.memory.db.FaceProfileObservationLinkEntity
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.UUID
import kotlin.math.min

class RoomFaceProfileStore(
    private val faceProfileDao: FaceProfileDao,
    private val personStore: PersonStore,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val idProvider: () -> String = { UUID.randomUUID().toString() }
) : FaceProfileStore {
    override suspend fun createProfileCandidate(
        label: String?,
        note: String?
    ): FaceProfileRecord {
        val now = nowProvider()
        val profile = FaceProfileRecord(
            profileId = idProvider(),
            status = FaceProfileStatus.CANDIDATE,
            label = label?.trim()?.ifBlank { null },
            note = note?.trim()?.ifBlank { null },
            linkedPersonId = null,
            createdAtMs = now,
            updatedAtMs = now
        )
        faceProfileDao.insertProfile(profile.toEntity())
        return profile
    }

    override suspend fun getProfile(profileId: String): FaceProfileRecord? {
        return faceProfileDao.getProfileById(profileId)?.toRecord()
    }

    override suspend fun listProfiles(): List<FaceProfileRecord> {
        return faceProfileDao.listProfiles().map { entity ->
            entity.toRecord()
        }
    }

    override suspend fun linkObservationToProfile(
        observationId: String,
        profileId: String
    ): Boolean {
        val normalizedObservationId = observationId.trim()
        if (normalizedObservationId.isBlank()) {
            return false
        }
        return faceProfileDao.linkObservationToProfile(
            profileId = profileId,
            observationId = normalizedObservationId,
            linkedAtMs = nowProvider()
        )
    }

    override suspend fun listProfileObservations(profileId: String): List<FaceProfileObservationLinkRecord> {
        return faceProfileDao.listObservationLinks(profileId).map { link ->
            link.toRecord()
        }
    }

    override suspend fun addEmbeddingToProfile(
        profileId: String,
        values: List<Float>,
        metadata: String?
    ): FaceProfileEmbeddingRecord? {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) {
            return null
        }
        if (!faceProfileDao.profileExists(normalizedProfileId)) {
            return null
        }
        val normalizedValues = values
            .asSequence()
            .filter { value -> value.isFinite() }
            .toList()
        if (normalizedValues.isEmpty() || normalizedValues.size != values.size) {
            return null
        }
        val record = FaceProfileEmbeddingRecord(
            embeddingId = idProvider(),
            profileId = normalizedProfileId,
            createdAtMs = nowProvider(),
            vectorDim = normalizedValues.size,
            values = normalizedValues,
            metadata = metadata?.trim()?.ifBlank { null }
        )
        faceProfileDao.insertEmbedding(record.toEntity())
        return record
    }

    override suspend fun getEmbedding(embeddingId: String): FaceProfileEmbeddingRecord? {
        return faceProfileDao.getEmbeddingById(embeddingId)?.toRecord()
    }

    override suspend fun listProfileEmbeddings(profileId: String): List<FaceProfileEmbeddingRecord> {
        return faceProfileDao.listEmbeddingsByProfile(profileId).map { entity ->
            entity.toRecord()
        }
    }

    override suspend fun linkProfileToPerson(profileId: String, personId: String): Boolean {
        val normalizedProfileId = profileId.trim()
        val normalizedPersonId = personId.trim()
        if (normalizedProfileId.isBlank() || normalizedPersonId.isBlank()) {
            return false
        }
        if (!faceProfileDao.profileExists(normalizedProfileId)) {
            return false
        }
        val person = personStore.getById(normalizedPersonId) ?: return false
        return faceProfileDao.setLinkedPerson(
            profileId = normalizedProfileId,
            personId = person.personId,
            updatedAtMs = nowProvider()
        ) > 0
    }

    override suspend fun unlinkProfileFromPerson(profileId: String): Boolean {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) {
            return false
        }
        return faceProfileDao.clearLinkedPerson(
            profileId = normalizedProfileId,
            updatedAtMs = nowProvider()
        ) > 0
    }

    override suspend fun getPersonForProfile(profileId: String): PersonRecord? {
        val normalizedProfileId = profileId.trim()
        if (normalizedProfileId.isBlank()) {
            return null
        }
        val linkedPersonId = faceProfileDao.getProfileById(normalizedProfileId)
            ?.linkedPersonId
            ?.trim()
            ?.ifBlank { null } ?: return null
        return personStore.getById(linkedPersonId)
    }

    override suspend fun listProfilesForPerson(personId: String): List<FaceProfileRecord> {
        val normalizedPersonId = personId.trim()
        if (normalizedPersonId.isBlank()) {
            return emptyList()
        }
        if (personStore.getById(normalizedPersonId) == null) {
            return emptyList()
        }
        return faceProfileDao.listProfilesForPerson(normalizedPersonId).map { entity ->
            entity.toRecord()
        }
    }

    override suspend fun recordKnownPersonSeenFromLinkedProfile(
        profileId: String,
        observationId: String,
        seenAtMs: Long
    ): LinkedProfileSeenPropagationResult {
        val normalizedProfileId = profileId.trim()
        val normalizedObservationId = observationId.trim()
        if (normalizedProfileId.isBlank() || normalizedObservationId.isBlank()) {
            return LinkedProfileSeenPropagationResult(
                status = LinkedProfileSeenPropagationStatus.PROFILE_NOT_FOUND,
                profileId = normalizedProfileId,
                observationId = normalizedObservationId
            )
        }

        val profile = faceProfileDao.getProfileById(normalizedProfileId)
            ?: return LinkedProfileSeenPropagationResult(
                status = LinkedProfileSeenPropagationStatus.PROFILE_NOT_FOUND,
                profileId = normalizedProfileId,
                observationId = normalizedObservationId
            )
        val hasLinkedObservation = faceProfileDao.hasObservationLink(
            profileId = normalizedProfileId,
            observationId = normalizedObservationId
        )
        if (!hasLinkedObservation) {
            return LinkedProfileSeenPropagationResult(
                status = LinkedProfileSeenPropagationStatus.OBSERVATION_NOT_LINKED,
                profileId = normalizedProfileId,
                observationId = normalizedObservationId
            )
        }

        val linkedPersonId = profile.linkedPersonId
            ?.trim()
            ?.ifBlank { null }
            ?: return LinkedProfileSeenPropagationResult(
                status = LinkedProfileSeenPropagationStatus.PROFILE_UNRESOLVED,
                profileId = normalizedProfileId,
                observationId = normalizedObservationId
            )

        val updatedPerson = personStore.recordPersonSeen(
            personId = linkedPersonId,
            seenAtMs = seenAtMs
        ) ?: return LinkedProfileSeenPropagationResult(
            status = LinkedProfileSeenPropagationStatus.PERSON_NOT_FOUND,
            profileId = normalizedProfileId,
            observationId = normalizedObservationId
        )

        return LinkedProfileSeenPropagationResult(
            status = LinkedProfileSeenPropagationStatus.SUCCESS,
            person = updatedPerson,
            profileId = normalizedProfileId,
            observationId = normalizedObservationId
        )
    }

    private fun FaceProfileRecord.toEntity(): FaceProfileEntity {
        return FaceProfileEntity(
            profileId = profileId,
            status = status.name,
            label = label,
            note = note,
            linkedPersonId = linkedPersonId,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }

    private fun FaceProfileEntity.toRecord(): FaceProfileRecord {
        return FaceProfileRecord(
            profileId = profileId,
            status = status.fromStatusName(),
            label = label,
            note = note,
            linkedPersonId = linkedPersonId,
            createdAtMs = createdAtMs,
            updatedAtMs = updatedAtMs
        )
    }

    private fun FaceProfileObservationLinkEntity.toRecord(): FaceProfileObservationLinkRecord {
        return FaceProfileObservationLinkRecord(
            profileId = profileId,
            observationId = observationId,
            linkedAtMs = linkedAtMs
        )
    }

    private fun FaceProfileEmbeddingRecord.toEntity(): FaceProfileEmbeddingEntity {
        return FaceProfileEmbeddingEntity(
            embeddingId = embeddingId,
            profileId = profileId,
            createdAtMs = createdAtMs,
            vectorBlob = values.toBlob(),
            vectorDim = vectorDim,
            metadata = metadata
        )
    }

    private fun FaceProfileEmbeddingEntity.toRecord(): FaceProfileEmbeddingRecord {
        val decodedValues = vectorBlob.toFloatList(vectorDim)
        val resolvedDim = if (vectorDim > 0) {
            min(vectorDim, decodedValues.size)
        } else {
            decodedValues.size
        }
        return FaceProfileEmbeddingRecord(
            embeddingId = embeddingId,
            profileId = profileId,
            createdAtMs = createdAtMs,
            vectorDim = resolvedDim,
            values = decodedValues.take(resolvedDim),
            metadata = metadata
        )
    }
}

private fun String.fromStatusName(): FaceProfileStatus {
    return FaceProfileStatus.entries.firstOrNull { status ->
        status.name == this
    } ?: FaceProfileStatus.CANDIDATE
}

private fun List<Float>.toBlob(): ByteArray {
    val buffer = ByteBuffer
        .allocate(size * Float.SIZE_BYTES)
        .order(ByteOrder.LITTLE_ENDIAN)
    for (value in this) {
        buffer.putFloat(value)
    }
    return buffer.array()
}

private fun ByteArray.toFloatList(expectedDim: Int): List<Float> {
    if (size < Float.SIZE_BYTES || size % Float.SIZE_BYTES != 0) {
        return emptyList()
    }
    val dimFromBlob = size / Float.SIZE_BYTES
    val dim = if (expectedDim > 0) min(expectedDim, dimFromBlob) else dimFromBlob
    val buffer = ByteBuffer.wrap(this).order(ByteOrder.LITTLE_ENDIAN)
    return buildList(dim) {
        repeat(dim) {
            add(buffer.float)
        }
    }
}
