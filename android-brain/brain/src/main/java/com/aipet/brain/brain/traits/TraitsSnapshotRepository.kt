package com.aipet.brain.brain.traits

import kotlinx.coroutines.flow.Flow

interface TraitsSnapshotRepository {
    suspend fun save(snapshot: TraitsSnapshot)

    suspend fun latest(): TraitsSnapshot?

    fun observeLatest(): Flow<TraitsSnapshot?>
}
