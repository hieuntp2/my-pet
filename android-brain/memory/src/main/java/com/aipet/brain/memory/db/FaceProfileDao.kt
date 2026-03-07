package com.aipet.brain.memory.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction

@Dao
interface FaceProfileDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertProfile(profile: FaceProfileEntity)

    @Query("SELECT * FROM face_profiles WHERE profile_id = :profileId LIMIT 1")
    suspend fun getProfileById(profileId: String): FaceProfileEntity?

    @Query("SELECT * FROM face_profiles ORDER BY updated_at_ms DESC, profile_id ASC")
    suspend fun listProfiles(): List<FaceProfileEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM face_profiles WHERE profile_id = :profileId)")
    suspend fun profileExists(profileId: String): Boolean

    @Query(
        """
        UPDATE face_profiles
        SET linked_person_id = :personId, updated_at_ms = :updatedAtMs
        WHERE profile_id = :profileId
        """
    )
    suspend fun setLinkedPerson(
        profileId: String,
        personId: String,
        updatedAtMs: Long
    ): Int

    @Query(
        """
        UPDATE face_profiles
        SET linked_person_id = NULL, updated_at_ms = :updatedAtMs
        WHERE profile_id = :profileId AND linked_person_id IS NOT NULL
        """
    )
    suspend fun clearLinkedPerson(profileId: String, updatedAtMs: Long): Int

    @Query(
        """
        SELECT * FROM face_profiles
        WHERE linked_person_id = :personId
        ORDER BY updated_at_ms DESC, profile_id ASC
        """
    )
    suspend fun listProfilesForPerson(personId: String): List<FaceProfileEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertObservationLink(link: FaceProfileObservationLinkEntity): Long

    @Query(
        """
        SELECT * FROM face_profile_observation_links
        WHERE profile_id = :profileId
        ORDER BY linked_at_ms DESC, observation_id ASC
        """
    )
    suspend fun listObservationLinks(profileId: String): List<FaceProfileObservationLinkEntity>

    @Query(
        """
        SELECT EXISTS(
            SELECT 1
            FROM face_profile_observation_links
            WHERE profile_id = :profileId AND observation_id = :observationId
        )
        """
    )
    suspend fun hasObservationLink(profileId: String, observationId: String): Boolean

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEmbedding(embedding: FaceProfileEmbeddingEntity)

    @Query("SELECT * FROM face_profile_embeddings WHERE embedding_id = :embeddingId LIMIT 1")
    suspend fun getEmbeddingById(embeddingId: String): FaceProfileEmbeddingEntity?

    @Query(
        """
        SELECT * FROM face_profile_embeddings
        WHERE profile_id = :profileId
        ORDER BY created_at_ms DESC, embedding_id ASC
        """
    )
    suspend fun listEmbeddingsByProfile(profileId: String): List<FaceProfileEmbeddingEntity>

    @Transaction
    suspend fun linkObservationToProfile(
        profileId: String,
        observationId: String,
        linkedAtMs: Long
    ): Boolean {
        if (!profileExists(profileId)) {
            return false
        }
        return insertObservationLink(
            FaceProfileObservationLinkEntity(
                profileId = profileId,
                observationId = observationId,
                linkedAtMs = linkedAtMs
            )
        ) != -1L
    }
}
