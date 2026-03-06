package com.aipet.brain.memory.events

import com.aipet.brain.brain.events.EventEnvelope
import kotlinx.coroutines.flow.Flow

interface EventStore {
    suspend fun save(event: EventEnvelope)

    suspend fun listLatest(limit: Int): List<EventEnvelope>

    fun observeLatest(limit: Int): Flow<List<EventEnvelope>>

    suspend fun clearAll()
}
