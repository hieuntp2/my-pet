package com.aipet.brain.memory.unknownfaces

interface UnknownFaceCandidateStore {
    suspend fun upsert(candidate: UnknownFaceCandidateRecord)

    suspend fun getById(candidateId: String): UnknownFaceCandidateRecord?

    suspend fun listActive(): List<UnknownFaceCandidateRecord>

    suspend fun listAll(): List<UnknownFaceCandidateRecord>

    suspend fun countActive(): Int

    suspend fun deleteResolvedOlderThan(olderThanMs: Long): Int
}
