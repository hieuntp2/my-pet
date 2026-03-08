package com.aipet.brain.memory.teachsamples

import com.aipet.brain.memory.db.TeachSampleDao
import com.aipet.brain.memory.db.TeachSampleEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomTeachSampleStore(
    private val teachSampleDao: TeachSampleDao
) : TeachSampleStore {
    override suspend fun insert(sample: TeachSampleRecord): Boolean {
        return teachSampleDao.insert(sample.toEntity()) != -1L
    }

    override suspend fun listBySession(sessionId: String, limit: Int): List<TeachSampleRecord> {
        return teachSampleDao.listBySession(
            sessionId = sessionId,
            limit = limit.coerceAtLeast(0)
        ).map { entity ->
            entity.toRecord()
        }
    }

    override fun observeBySession(sessionId: String, limit: Int): Flow<List<TeachSampleRecord>> {
        return teachSampleDao.observeBySession(
            sessionId = sessionId,
            limit = limit.coerceAtLeast(0)
        ).map { entities ->
            entities.map { entity -> entity.toRecord() }
        }
    }

    override suspend fun deleteBySessionAndObservation(
        sessionId: String,
        observationId: String
    ): Boolean {
        return teachSampleDao.deleteBySessionAndObservation(
            sessionId = sessionId,
            observationId = observationId
        ) > 0
    }

    private fun TeachSampleRecord.toEntity(): TeachSampleEntity {
        return TeachSampleEntity(
            sampleId = sampleId,
            sessionId = sessionId,
            observationId = observationId,
            observedAtMs = observedAtMs,
            source = source,
            note = note,
            imageUri = imageUri,
            faceCropUri = faceCropUri,
            qualityStatus = qualityMetadata.qualityStatus.name,
            qualityFlags = qualityMetadata.qualityFlags
                .map { flag -> flag.name }
                .sorted()
                .joinToString(separator = ","),
            qualityNote = qualityMetadata.note,
            qualityEvaluatedAtMs = qualityMetadata.evaluatedAtMs,
            createdAtMs = createdAtMs
        )
    }

    private fun TeachSampleEntity.toRecord(): TeachSampleRecord {
        return TeachSampleRecord(
            sampleId = sampleId,
            sessionId = sessionId,
            observationId = observationId,
            observedAtMs = observedAtMs,
            source = source,
            note = note,
            imageUri = imageUri,
            faceCropUri = faceCropUri,
            qualityMetadata = SampleQualityMetadata(
                qualityStatus = qualityStatus.toSampleQualityStatus(),
                qualityFlags = qualityFlags.toSampleQualityFlags(),
                note = qualityNote,
                evaluatedAtMs = qualityEvaluatedAtMs
            ),
            createdAtMs = createdAtMs
        )
    }

    private fun String.toSampleQualityStatus(): SampleQualityStatus {
        return runCatching {
            SampleQualityStatus.valueOf(this)
        }.getOrDefault(SampleQualityStatus.UNASSESSED)
    }

    private fun String.toSampleQualityFlags(): Set<SampleQualityFlag> {
        if (isBlank()) {
            return emptySet()
        }
        return split(",")
            .asSequence()
            .map { item -> item.trim() }
            .filter { item -> item.isNotBlank() }
            .mapNotNull { item ->
                runCatching {
                    SampleQualityFlag.valueOf(item)
                }.getOrNull()
            }
            .toSet()
    }
}
