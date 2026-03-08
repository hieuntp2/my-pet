package com.aipet.brain.memory.teachsessioncompletion

import com.aipet.brain.memory.db.TeachSessionCompletionDao
import com.aipet.brain.memory.db.TeachSessionCompletionEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

class RoomTeachSessionCompletionStore(
    private val teachSessionCompletionDao: TeachSessionCompletionDao,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) : TeachSessionCompletionStore {
    override suspend fun confirmCompletion(teachSessionId: String, confirmedAtMs: Long): Boolean {
        val normalizedSessionId = teachSessionId.trim()
        if (normalizedSessionId.isBlank()) {
            return false
        }
        val nowMs = nowProvider()
        val resolvedConfirmedAtMs = if (confirmedAtMs > 0L) {
            confirmedAtMs
        } else {
            nowMs
        }
        teachSessionCompletionDao.upsert(
            TeachSessionCompletionEntity(
                teachSessionId = normalizedSessionId,
                isCompletedConfirmed = true,
                confirmedAtMs = resolvedConfirmedAtMs,
                updatedAtMs = nowMs
            )
        )
        return true
    }

    override suspend fun getBySessionId(teachSessionId: String): TeachSessionCompletionRecord? {
        val normalizedSessionId = teachSessionId.trim()
        if (normalizedSessionId.isBlank()) {
            return null
        }
        return teachSessionCompletionDao.getBySessionId(normalizedSessionId)?.toRecord()
    }

    override fun observeBySessionId(teachSessionId: String): Flow<TeachSessionCompletionRecord?> {
        val normalizedSessionId = teachSessionId.trim()
        if (normalizedSessionId.isBlank()) {
            return flowOf(null)
        }
        return teachSessionCompletionDao.observeBySessionId(normalizedSessionId).map { entity ->
            entity?.toRecord()
        }
    }

    override suspend fun clearCompletion(teachSessionId: String): Boolean {
        val normalizedSessionId = teachSessionId.trim()
        if (normalizedSessionId.isBlank()) {
            return false
        }
        return teachSessionCompletionDao.deleteBySessionId(normalizedSessionId) > 0
    }

    private fun TeachSessionCompletionEntity.toRecord(): TeachSessionCompletionRecord {
        return TeachSessionCompletionRecord(
            teachSessionId = teachSessionId,
            isCompletedConfirmed = isCompletedConfirmed,
            confirmedAtMs = confirmedAtMs,
            updatedAtMs = updatedAtMs
        )
    }
}
