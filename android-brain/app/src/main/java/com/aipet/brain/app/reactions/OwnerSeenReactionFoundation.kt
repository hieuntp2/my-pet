package com.aipet.brain.app.reactions

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.OwnerSeenEventPayload
import com.aipet.brain.brain.events.PersonSeenEventPayload
import com.aipet.brain.brain.events.RobotGreetingOwnerEventPayload
import com.aipet.brain.memory.persons.PersonRecord
import kotlinx.coroutines.flow.collect

internal enum class PersonSeenSource {
    DIRECT_PERSON_DEBUG_ACTION,
    LINKED_PROFILE_OBSERVATION_BRIDGE
}

internal class PersonSeenEventPublisher(
    private val eventBus: EventBus
) {
    suspend fun publishPersonSeen(
        person: PersonRecord,
        source: PersonSeenSource,
        profileId: String? = null,
        observationId: String? = null
    ) {
        val seenAtMs = person.lastSeenAtMs ?: person.updatedAtMs
        val payload = PersonSeenEventPayload(
            personId = person.personId,
            seenAtMs = seenAtMs,
            seenCount = person.seenCount,
            isOwner = person.isOwner,
            source = source.name,
            profileId = profileId,
            observationId = observationId
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_SEEN_RECORDED,
                payloadJson = payload.toJson(),
                timestampMs = seenAtMs
            )
        )
    }
}

internal data class OwnerSeenReaction(
    val personId: String,
    val seenAtMs: Long,
    val seenCount: Int
)

internal class OwnerSeenReactionEngine(
    private val eventBus: EventBus,
    private val greetingMessage: String = "owner_greeting_triggered"
) {
    suspend fun observePersonSeenUpdates() {
        eventBus.observe().collect { event ->
            if (event.type != EventType.PERSON_SEEN_RECORDED) {
                return@collect
            }
            val personSeen = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return@collect
            if (!personSeen.isOwner) {
                return@collect
            }
            emitOwnerSeenReaction(
                OwnerSeenReaction(
                    personId = personSeen.personId,
                    seenAtMs = personSeen.seenAtMs,
                    seenCount = personSeen.seenCount
                )
            )
        }
    }

    private suspend fun emitOwnerSeenReaction(reaction: OwnerSeenReaction) {
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.OWNER_SEEN_DETECTED,
                timestampMs = reaction.seenAtMs,
                payloadJson = OwnerSeenEventPayload(
                    personId = reaction.personId,
                    seenAtMs = reaction.seenAtMs,
                    seenCount = reaction.seenCount
                ).toJson()
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.ROBOT_GREETING_OWNER_TRIGGERED,
                timestampMs = reaction.seenAtMs,
                payloadJson = RobotGreetingOwnerEventPayload(
                    personId = reaction.personId,
                    seenAtMs = reaction.seenAtMs,
                    message = greetingMessage
                ).toJson()
            )
        )
    }
}
