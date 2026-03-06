package com.aipet.brain.brain.events

import kotlinx.coroutines.flow.Flow

interface EventBus {
    suspend fun publish(event: EventEnvelope)

    fun observe(): Flow<EventEnvelope>
}
