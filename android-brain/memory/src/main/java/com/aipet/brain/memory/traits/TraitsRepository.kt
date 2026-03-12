package com.aipet.brain.memory.traits

import com.aipet.brain.memory.db.TraitsSnapshotEntity

interface TraitsRepository {
    suspend fun saveSnapshot(snapshot: TraitsSnapshotEntity)

    suspend fun getLatestSnapshot(): TraitsSnapshotEntity?
}
