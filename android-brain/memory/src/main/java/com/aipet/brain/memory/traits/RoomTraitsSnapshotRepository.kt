package com.aipet.brain.memory.traits

import com.aipet.brain.brain.traits.TraitsSnapshot
import com.aipet.brain.brain.traits.TraitsSnapshotRepository
import com.aipet.brain.memory.db.TraitsSnapshotDao
import com.aipet.brain.memory.db.TraitsSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTraitsSnapshotRepository(
    private val dao: TraitsSnapshotDao
) : TraitsSnapshotRepository {
    override suspend fun save(snapshot: TraitsSnapshot) {
        dao.insert(snapshot.toEntity())
    }

    override suspend fun latest(): TraitsSnapshot? {
        return dao.getLatest()?.toDomain()
    }

    override fun observeLatest(): Flow<TraitsSnapshot?> {
        return dao.observeLatest().map { entity ->
            entity?.toDomain()
        }
    }

    private fun TraitsSnapshot.toEntity(): TraitsSnapshotEntity {
        return TraitsSnapshotEntity(
            curiosity = curiosity,
            sociability = sociability,
            energy = energy,
            patience = patience,
            boldness = boldness,
            createdAt = capturedAtMs
        )
    }

    private fun TraitsSnapshotEntity.toDomain(): TraitsSnapshot {
        return TraitsSnapshot(
            snapshotId = id.toString(),
            capturedAtMs = createdAt,
            curiosity = curiosity,
            sociability = sociability,
            energy = energy,
            patience = patience,
            boldness = boldness
        )
    }
}
