package com.aipet.brain.app.ui.profiles

import com.aipet.brain.memory.profiles.FaceProfileObservationLinkRecord
import com.aipet.brain.memory.profiles.FaceProfileEmbeddingRecord
import com.aipet.brain.memory.profiles.FaceProfileRecord
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.memory.profiles.LinkedProfileSeenPropagationResult
import com.aipet.brain.memory.persons.PersonRecord
import com.aipet.brain.memory.persons.PersonStore

internal class ProfileAssociationController(
    private val faceProfileStore: FaceProfileStore,
    private val personStore: PersonStore
) {
    suspend fun createProfileCandidate(
        label: String?,
        note: String?
    ): FaceProfileRecord {
        return faceProfileStore.createProfileCandidate(
            label = label,
            note = note
        )
    }

    suspend fun listProfiles(): List<FaceProfileRecord> {
        return faceProfileStore.listProfiles()
    }

    suspend fun getProfile(profileId: String): FaceProfileRecord? {
        return faceProfileStore.getProfile(profileId)
    }

    suspend fun linkObservationToProfile(
        observationId: String,
        profileId: String
    ): Boolean {
        return faceProfileStore.linkObservationToProfile(
            observationId = observationId,
            profileId = profileId
        )
    }

    suspend fun listProfileObservations(profileId: String): List<FaceProfileObservationLinkRecord> {
        return faceProfileStore.listProfileObservations(profileId)
    }

    suspend fun addEmbeddingToProfile(
        profileId: String,
        values: List<Float>,
        metadata: String?
    ): FaceProfileEmbeddingRecord? {
        return faceProfileStore.addEmbeddingToProfile(
            profileId = profileId,
            values = values,
            metadata = metadata
        )
    }

    suspend fun getEmbedding(embeddingId: String): FaceProfileEmbeddingRecord? {
        return faceProfileStore.getEmbedding(embeddingId)
    }

    suspend fun listProfileEmbeddings(profileId: String): List<FaceProfileEmbeddingRecord> {
        return faceProfileStore.listProfileEmbeddings(profileId)
    }

    suspend fun listPersons(): List<PersonRecord> {
        return personStore.listAll()
    }

    suspend fun linkProfileToPerson(profileId: String, personId: String): Boolean {
        return faceProfileStore.linkProfileToPerson(
            profileId = profileId,
            personId = personId
        )
    }

    suspend fun unlinkProfileFromPerson(profileId: String): Boolean {
        return faceProfileStore.unlinkProfileFromPerson(profileId)
    }

    suspend fun getPersonForProfile(profileId: String): PersonRecord? {
        return faceProfileStore.getPersonForProfile(profileId)
    }

    suspend fun listProfilesForPerson(personId: String): List<FaceProfileRecord> {
        return faceProfileStore.listProfilesForPerson(personId)
    }

    suspend fun recordKnownPersonSeenFromLinkedProfile(
        profileId: String,
        observationId: String,
        seenAtMs: Long = System.currentTimeMillis()
    ): LinkedProfileSeenPropagationResult {
        return faceProfileStore.recordKnownPersonSeenFromLinkedProfile(
            profileId = profileId,
            observationId = observationId,
            seenAtMs = seenAtMs
        )
    }
}
