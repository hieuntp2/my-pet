package com.aipet.brain.memory.traits

import com.aipet.brain.brain.traits.TraitsSnapshot
import com.aipet.brain.memory.db.TraitsSnapshotDao
import com.aipet.brain.memory.db.TraitsSnapshotEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class RoomTraitsSnapshotRepositoryTest {
    @Test
    fun saveAndLatest_roundTripsSnapshot() = runTest {
        val repository = RoomTraitsSnapshotRepository(FakeTraitsSnapshotDao())
        val snapshot = testSnapshot(
            snapshotId = "snapshot-1",
            capturedAtMs = 1_000L
        )

        repository.save(snapshot)
        val loaded = repository.latest()

        assertNotNull(loaded)
        assertEquals(snapshot, loaded)
    }

    @Test
    fun latest_returnsMostRecentSnapshot() = runTest {
        val repository = RoomTraitsSnapshotRepository(FakeTraitsSnapshotDao())
        val older = testSnapshot(snapshotId = "older", capturedAtMs = 1_000L)
        val newer = testSnapshot(snapshotId = "newer", capturedAtMs = 2_000L)

        repository.save(older)
        repository.save(newer)

        assertEquals("newer", repository.latest()?.snapshotId)
    }
}

private class FakeTraitsSnapshotDao : TraitsSnapshotDao {
    private val snapshotsById = linkedMapOf<String, TraitsSnapshotEntity>()
    private val latestFlow = MutableStateFlow<TraitsSnapshotEntity?>(null)

    override suspend fun insert(snapshot: TraitsSnapshotEntity) {
        snapshotsById[snapshot.snapshotId] = snapshot
        latestFlow.value = latest()
    }

    override suspend fun latest(): TraitsSnapshotEntity? {
        return snapshotsById.values.maxWithOrNull(
            compareBy<TraitsSnapshotEntity> { it.capturedAtMs }
                .thenBy { it.snapshotId }
        )
    }

    override fun observeLatest(): Flow<TraitsSnapshotEntity?> {
        return latestFlow
    }
}

private fun testSnapshot(
    snapshotId: String,
    capturedAtMs: Long
): TraitsSnapshot {
    return TraitsSnapshot(
        snapshotId = snapshotId,
        capturedAtMs = capturedAtMs,
        curiosity = 0.5f,
        sociability = 0.4f,
        energy = 0.6f,
        patience = 0.5f,
        boldness = 0.3f
    )
}
