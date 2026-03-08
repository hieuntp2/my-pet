package com.aipet.brain.app.reactions

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class OwnerSeenReactionEngineTest {
    @Test
    fun ownerSeenEvent_emitsOwnerDetectedAndGreetingEvents() = runTest {
        val eventBus = FakeEventBus()
        val engine = OwnerSeenReactionEngine(
            eventBus = eventBus,
            greetingMessage = "hello_owner"
        )
        val observerJob = launch {
            engine.observePersonSeenUpdates()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 1_000L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-owner",
                    seenAtMs = 1_000L,
                    seenCount = 3,
                    isOwner = true,
                    source = PersonSeenSource.DIRECT_PERSON_DEBUG_ACTION.name
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(
            listOf(
                EventType.PERSON_SEEN_RECORDED,
                EventType.OWNER_SEEN_DETECTED,
                EventType.ROBOT_GREETING_OWNER_TRIGGERED
            ),
            eventBus.publishedEvents.map { event -> event.type }
        )

        observerJob.cancel()
    }

    @Test
    fun nonOwnerSeenEvent_doesNotEmitGreetingEvents() = runTest {
        val eventBus = FakeEventBus()
        val engine = OwnerSeenReactionEngine(eventBus = eventBus)
        val observerJob = launch {
            engine.observePersonSeenUpdates()
        }
        advanceUntilIdle()

        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                timestampMs = 1_500L,
                payloadJson = PersonSeenEventPayload(
                    personId = "person-non-owner",
                    seenAtMs = 1_500L,
                    seenCount = 1,
                    isOwner = false,
                    source = PersonSeenSource.LINKED_PROFILE_OBSERVATION_BRIDGE.name
                ).toJson()
            )
        )
        advanceUntilIdle()

        assertEquals(
            listOf(EventType.PERSON_SEEN_RECORDED),
            eventBus.publishedEvents.map { event -> event.type }
        )

        observerJob.cancel()
    }
}

private class FakeEventBus : EventBus {
    private val eventsFlow = MutableSharedFlow<EventEnvelope>(
        replay = 0,
        extraBufferCapacity = 32
    )
    val publishedEvents = mutableListOf<EventEnvelope>()

    override suspend fun publish(event: EventEnvelope) {
        publishedEvents.add(event)
        eventsFlow.emit(event)
    }

    override fun observe(): Flow<EventEnvelope> {
        return eventsFlow.asSharedFlow()
    }
}
