package com.aipet.brain.memory.teachsamples

import kotlinx.coroutines.flow.Flow

interface TeachSampleStore {
    suspend fun insert(sample: TeachSampleRecord): Boolean

    suspend fun listBySession(sessionId: String, limit: Int): List<TeachSampleRecord>

    fun observeBySession(sessionId: String, limit: Int): Flow<List<TeachSampleRecord>>

    suspend fun deleteBySessionAndObservation(sessionId: String, observationId: String): Boolean
}
