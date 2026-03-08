package com.aipet.brain.memory.teachsessioncompletion

import kotlinx.coroutines.flow.Flow

interface TeachSessionCompletionStore {
    suspend fun confirmCompletion(teachSessionId: String, confirmedAtMs: Long): Boolean

    suspend fun getBySessionId(teachSessionId: String): TeachSessionCompletionRecord?

    fun observeBySessionId(teachSessionId: String): Flow<TeachSessionCompletionRecord?>

    suspend fun clearCompletion(teachSessionId: String): Boolean
}
