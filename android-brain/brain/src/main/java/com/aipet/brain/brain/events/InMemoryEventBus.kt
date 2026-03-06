package com.aipet.brain.brain.events

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext

class InMemoryEventBus(
    private val persistEvent: (suspend (EventEnvelope) -> Unit)? = null,
    private val persistenceDispatcher: CoroutineDispatcher = Dispatchers.IO
) : EventBus {
    private val eventFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 64
    )

    override suspend fun publish(event: EventEnvelope) {
        eventFlow.emit(event)
        val persist = persistEvent ?: return
        withContext(persistenceDispatcher) {
            persist(event)
        }
    }

    override fun observe(): Flow<EventEnvelope> {
        return eventFlow.asSharedFlow()
    }
}
