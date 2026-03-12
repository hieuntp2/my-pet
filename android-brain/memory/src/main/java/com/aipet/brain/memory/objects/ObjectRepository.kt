package com.aipet.brain.memory.objects

import com.aipet.brain.memory.db.ObjectDao
import com.aipet.brain.memory.db.ObjectEntity
import java.util.UUID

class ObjectRepository(
    private val objectDao: ObjectDao,
    private val idGenerator: () -> String = { UUID.randomUUID().toString() },
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    suspend fun getAllObjects(): List<ObjectRecord> {
        return objectDao.getAllObjects().map { entity ->
            entity.toRecord()
        }
    }

    suspend fun listRecentSeenObjects(limit: Int): List<ObjectRecord> {
        if (limit <= 0) {
            return emptyList()
        }
        return objectDao.getRecentSeenObjects(limit = limit).map { entity ->
            entity.toRecord()
        }
    }

    suspend fun getObjectById(objectId: String): ObjectRecord? {
        val normalizedObjectId = objectId.trim()
        if (normalizedObjectId.isBlank()) {
            return null
        }
        return objectDao.getObjectById(normalizedObjectId)?.toRecord()
    }

    suspend fun createObject(name: String): ObjectRecord? {
        val normalizedName = name.trim()
        if (normalizedName.isBlank()) {
            return null
        }

        val objectId = idGenerator().trim()
        if (objectId.isBlank()) {
            return null
        }
        val createdAtMs = nowProvider()

        objectDao.insertObject(
            ObjectEntity(
                objectId = objectId,
                name = normalizedName,
                createdAtMs = createdAtMs,
                lastSeenAtMs = null,
                seenCount = 0
            )
        )
        return objectDao.getObjectById(objectId)?.toRecord()
    }

    suspend fun recordKnownObjectSeen(
        label: String,
        seenAtMs: Long = nowProvider()
    ): ObjectSeenUpdateResult? {
        val normalizedLabel = label.trim()
        if (normalizedLabel.isBlank()) {
            return null
        }

        val matched = objectDao.getLatestObjectByName(normalizedLabel) ?: return null
        val lastSeenAtMs = matched.lastSeenAtMs
        val shouldSkipUpdate = lastSeenAtMs != null && (
            seenAtMs <= lastSeenAtMs ||
                seenAtMs - lastSeenAtMs < MIN_OBJECT_SEEN_UPDATE_INTERVAL_MS
            )
        if (shouldSkipUpdate) {
            return ObjectSeenUpdateResult(
                objectRecord = matched.toRecord(),
                wasUpdated = false
            )
        }

        val updated = objectDao.incrementSeenStats(
            objectId = matched.objectId,
            seenAtMs = seenAtMs
        ) > 0
        val latest = objectDao.getObjectById(matched.objectId)?.toRecord()
            ?: matched.toRecord()
        return ObjectSeenUpdateResult(
            objectRecord = latest,
            wasUpdated = updated
        )
    }

    suspend fun resolveKnownObjectDisplayName(
        detectedCanonicalLabel: String
    ): String? {
        val normalizedLabel = detectedCanonicalLabel.trim()
        if (normalizedLabel.isBlank()) {
            return null
        }
        val matched = objectDao.getLatestObjectByName(normalizedLabel) ?: return null
        val preferredDisplayName = matched.name.trim()
        return preferredDisplayName.takeIf { name -> name.isNotBlank() }
    }

    suspend fun deleteObject(objectId: String): Boolean {
        val normalizedObjectId = objectId.trim()
        if (normalizedObjectId.isBlank()) {
            return false
        }
        return objectDao.deleteObject(normalizedObjectId) > 0
    }

    private fun ObjectEntity.toRecord(): ObjectRecord {
        return ObjectRecord(
            objectId = objectId,
            name = name,
            createdAtMs = createdAtMs,
            lastSeenAtMs = lastSeenAtMs,
            seenCount = seenCount
        )
    }

    companion object {
        private const val MIN_OBJECT_SEEN_UPDATE_INTERVAL_MS = 1_500L
    }
}

data class ObjectSeenUpdateResult(
    val objectRecord: ObjectRecord,
    val wasUpdated: Boolean
)
