package com.aipet.brain.memory.profiles

import com.aipet.brain.memory.persons.PersonRecord

interface FaceProfileStore {
    suspend fun createProfileCandidate(
        label: String? = null,
        note: String? = null
    ): FaceProfileRecord

    suspend fun getProfile(profileId: String): FaceProfileRecord?

    suspend fun listProfiles(): List<FaceProfileRecord>

    suspend fun linkObservationToProfile(
        observationId: String,
        profileId: String
    ): Boolean

    suspend fun listProfileObservations(profileId: String): List<FaceProfileObservationLinkRecord>

    suspend fun addEmbeddingToProfile(
        profileId: String,
        values: List<Float>,
        metadata: String? = null
    ): FaceProfileEmbeddingRecord?

    suspend fun getEmbedding(embeddingId: String): FaceProfileEmbeddingRecord?

    suspend fun listProfileEmbeddings(profileId: String): List<FaceProfileEmbeddingRecord>

    suspend fun linkProfileToPerson(profileId: String, personId: String): Boolean

    suspend fun unlinkProfileFromPerson(profileId: String): Boolean

    suspend fun getPersonForProfile(profileId: String): PersonRecord?

    suspend fun listProfilesForPerson(personId: String): List<FaceProfileRecord>

    suspend fun recordKnownPersonSeenFromLinkedProfile(
        profileId: String,
        observationId: String,
        seenAtMs: Long = System.currentTimeMillis()
    ): LinkedProfileSeenPropagationResult
}

enum class LinkedProfileSeenPropagationStatus {
    SUCCESS,
    PROFILE_NOT_FOUND,
    OBSERVATION_NOT_LINKED,
    PROFILE_UNRESOLVED,
    PERSON_NOT_FOUND
}

data class LinkedProfileSeenPropagationResult(
    val status: LinkedProfileSeenPropagationStatus,
    val person: PersonRecord? = null,
    val profileId: String,
    val observationId: String
) {
    val propagated: Boolean
        get() = status == LinkedProfileSeenPropagationStatus.SUCCESS
}
