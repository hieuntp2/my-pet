package com.aipet.brain.memory.traits

import com.aipet.brain.memory.db.TraitsSnapshotDao
import com.aipet.brain.memory.db.TraitsSnapshotEntity

class TraitsRepositoryImpl(
    private val traitsSnapshotDao: TraitsSnapshotDao
) : TraitsRepository {
    override suspend fun saveSnapshot(snapshot: TraitsSnapshotEntity) {
        traitsSnapshotDao.insert(snapshot)
    }

    override suspend fun getLatestSnapshot(): TraitsSnapshotEntity? {
        return traitsSnapshotDao.getLatest()
    }
}
