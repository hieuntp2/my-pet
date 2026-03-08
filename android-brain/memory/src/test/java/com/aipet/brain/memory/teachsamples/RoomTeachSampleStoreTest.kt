package com.aipet.brain.memory.teachsamples

import com.aipet.brain.memory.db.TeachSampleDao
import com.aipet.brain.memory.db.TeachSampleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTeachSampleStoreTest {
    @Test
    fun insert_andListBySession_persistsCameraBackedImageAndFaceCropUriAndQualityMetadataReference() = runTest {
        val store = RoomTeachSampleStore(FakeTeachSampleDao())

        val inserted = store.insert(
            TeachSampleRecord(
                sampleId = "sample-1",
                sessionId = "session-a",
                observationId = "observation-1",
                observedAtMs = 1_000L,
                source = "CAMERA",
                note = "note-1",
                imageUri = "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-1.jpg",
                faceCropUri = "content://com.aipet.brain.app.fileprovider/teach_samples/face_crops/sample-1.jpg",
                qualityMetadata = SampleQualityMetadata(
                    qualityStatus = SampleQualityStatus.UNASSESSED,
                    qualityFlags = emptySet(),
                    note = "camera-captured reference",
                    evaluatedAtMs = 1_050L
                ),
                createdAtMs = 1_100L
            )
        )
        val listed = store.listBySession(sessionId = "session-a", limit = 10)

        assertTrue(inserted)
        assertEquals(1, listed.size)
        assertEquals(
            "content://com.aipet.brain.app.fileprovider/teach_samples/camera/sample-1.jpg",
            listed.first().imageUri
        )
        assertEquals(
            "content://com.aipet.brain.app.fileprovider/teach_samples/face_crops/sample-1.jpg",
            listed.first().faceCropUri
        )
        assertEquals(SampleQualityStatus.UNASSESSED, listed.first().qualityMetadata.qualityStatus)
        assertEquals(emptySet<SampleQualityFlag>(), listed.first().qualityMetadata.qualityFlags)
        assertEquals("camera-captured reference", listed.first().qualityMetadata.note)
        assertEquals(1_050L, listed.first().qualityMetadata.evaluatedAtMs)
    }

    @Test
    fun observeBySession_returnsOnlyRequestedSessionRecords_withQualityMetadata() = runTest {
        val dao = FakeTeachSampleDao()
        val store = RoomTeachSampleStore(dao)
        dao.insert(
            TeachSampleEntity(
                sampleId = "sample-1",
                sessionId = "session-a",
                observationId = "observation-1",
                observedAtMs = 1_000L,
                source = "DEBUG",
                note = "note-1",
                imageUri = "file:///tmp/sample-1.png",
                faceCropUri = "file:///tmp/sample-1-crop.png",
                qualityStatus = SampleQualityStatus.LIMITED_SOURCE.name,
                qualityFlags = SampleQualityFlag.DEBUG_GENERATED_IMAGE.name,
                qualityNote = "quality-note-1",
                qualityEvaluatedAtMs = 1_080L,
                createdAtMs = 1_100L
            )
        )
        dao.insert(
            TeachSampleEntity(
                sampleId = "sample-2",
                sessionId = "session-b",
                observationId = "observation-2",
                observedAtMs = 2_000L,
                source = "DEBUG",
                note = "note-2",
                imageUri = "file:///tmp/sample-2.png",
                faceCropUri = null,
                qualityStatus = SampleQualityStatus.UNASSESSED.name,
                qualityFlags = "",
                qualityNote = null,
                qualityEvaluatedAtMs = null,
                createdAtMs = 2_100L
            )
        )

        val observed = store.observeBySession(
            sessionId = "session-a",
            limit = 10
        ).first()

        assertEquals(1, observed.size)
        assertEquals("observation-1", observed.first().observationId)
        assertEquals("file:///tmp/sample-1-crop.png", observed.first().faceCropUri)
        assertEquals(SampleQualityStatus.LIMITED_SOURCE, observed.first().qualityMetadata.qualityStatus)
        assertEquals(
            setOf(SampleQualityFlag.DEBUG_GENERATED_IMAGE),
            observed.first().qualityMetadata.qualityFlags
        )
        assertEquals("quality-note-1", observed.first().qualityMetadata.note)
        assertEquals(1_080L, observed.first().qualityMetadata.evaluatedAtMs)
    }

    @Test
    fun deleteBySessionAndObservation_removesTargetSampleFromSessionSourceOfTruth() = runTest {
        val dao = FakeTeachSampleDao()
        val store = RoomTeachSampleStore(dao)
        store.insert(
            TeachSampleRecord(
                sampleId = "sample-1",
                sessionId = "session-a",
                observationId = "observation-1",
                observedAtMs = 1_000L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/1.jpg",
                faceCropUri = "content://sample/1-crop.jpg",
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 1_010L
            )
        )
        store.insert(
            TeachSampleRecord(
                sampleId = "sample-2",
                sessionId = "session-a",
                observationId = "observation-2",
                observedAtMs = 1_020L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/2.jpg",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 1_030L
            )
        )
        store.insert(
            TeachSampleRecord(
                sampleId = "sample-3",
                sessionId = "session-b",
                observationId = "observation-3",
                observedAtMs = 1_040L,
                source = "CAMERA",
                note = null,
                imageUri = "content://sample/3.jpg",
                faceCropUri = null,
                qualityMetadata = SampleQualityMetadata.default,
                createdAtMs = 1_050L
            )
        )

        val deleted = store.deleteBySessionAndObservation(
            sessionId = "session-a",
            observationId = "observation-1"
        )
        val remainingSessionA = store.listBySession(sessionId = "session-a", limit = 10)
        val remainingSessionB = store.listBySession(sessionId = "session-b", limit = 10)
        val observedSessionA = store.observeBySession(
            sessionId = "session-a",
            limit = 10
        ).first()

        assertTrue(deleted)
        assertEquals(listOf("observation-2"), remainingSessionA.map { record -> record.observationId })
        assertEquals(listOf("observation-3"), remainingSessionB.map { record -> record.observationId })
        assertEquals(listOf("observation-2"), observedSessionA.map { record -> record.observationId })
    }
}

private class FakeTeachSampleDao : TeachSampleDao {
    private val records = mutableListOf<TeachSampleEntity>()
    private val flow = MutableStateFlow<List<TeachSampleEntity>>(emptyList())

    override suspend fun insert(sample: TeachSampleEntity): Long {
        val alreadyExists = records.any { existing ->
            existing.observationId == sample.observationId
        }
        if (alreadyExists) {
            return -1L
        }
        records.add(sample)
        flow.value = records.toList()
        return 1L
    }

    override suspend fun listBySession(sessionId: String, limit: Int): List<TeachSampleEntity> {
        return records
            .asSequence()
            .filter { entity -> entity.sessionId == sessionId }
            .sortedWith(
                compareByDescending<TeachSampleEntity> { entity -> entity.createdAtMs }
                    .thenBy { entity -> entity.sampleId }
            )
            .take(limit.coerceAtLeast(0))
            .toList()
    }

    override fun observeBySession(sessionId: String, limit: Int): Flow<List<TeachSampleEntity>> {
        return flow.map { entities ->
            entities
                .asSequence()
                .filter { entity -> entity.sessionId == sessionId }
                .sortedWith(
                    compareByDescending<TeachSampleEntity> { entity -> entity.createdAtMs }
                        .thenBy { entity -> entity.sampleId }
                )
                .take(limit.coerceAtLeast(0))
                .toList()
        }
    }

    override suspend fun deleteBySessionAndObservation(sessionId: String, observationId: String): Int {
        val beforeCount = records.size
        records.removeAll { entity ->
            entity.sessionId == sessionId && entity.observationId == observationId
        }
        val removedCount = beforeCount - records.size
        if (removedCount > 0) {
            flow.value = records.toList()
        }
        return removedCount
    }
}
