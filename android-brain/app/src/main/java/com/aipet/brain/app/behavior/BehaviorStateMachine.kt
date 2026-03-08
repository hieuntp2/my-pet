package com.aipet.brain.app.behavior

import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonSeenEventPayload
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect

internal enum class RobotBehaviorState {
    IDLE,
    ATTENTIVE,
    GREETING_OWNER
}

internal enum class BehaviorTransitionReason {
    OWNER_SEEN,
    NON_OWNER_SEEN,
    OWNER_GREETING_TRIGGERED
}

internal data class BehaviorStateTransition(
    val fromState: RobotBehaviorState,
    val toState: RobotBehaviorState,
    val reason: BehaviorTransitionReason,
    val timestampMs: Long
)

internal data class BehaviorStateSnapshot(
    val currentState: RobotBehaviorState,
    val updatedAtMs: Long,
    val lastTransition: BehaviorStateTransition?
)

internal class BehaviorStateMachine(
    initialState: RobotBehaviorState = RobotBehaviorState.IDLE,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val state = MutableStateFlow(
        BehaviorStateSnapshot(
            currentState = initialState,
            updatedAtMs = nowProvider(),
            lastTransition = null
        )
    )

    fun getCurrentBehaviorState(): BehaviorStateSnapshot {
        return state.value
    }

    fun observeBehaviorState(): StateFlow<BehaviorStateSnapshot> {
        return state.asStateFlow()
    }

    fun transitionTo(
        targetState: RobotBehaviorState,
        reason: BehaviorTransitionReason,
        timestampMs: Long = nowProvider()
    ): Boolean {
        val current = state.value
        if (current.currentState == targetState) {
            return false
        }
        state.value = BehaviorStateSnapshot(
            currentState = targetState,
            updatedAtMs = timestampMs,
            lastTransition = BehaviorStateTransition(
                fromState = current.currentState,
                toState = targetState,
                reason = reason,
                timestampMs = timestampMs
            )
        )
        return true
    }

    fun onEvent(event: EventEnvelope) {
        when (event.type) {
            EventType.PERSON_SEEN_RECORDED -> handlePersonSeenEvent(event)
            EventType.ROBOT_GREETING_OWNER_TRIGGERED -> {
                transitionTo(
                    targetState = RobotBehaviorState.GREETING_OWNER,
                    reason = BehaviorTransitionReason.OWNER_GREETING_TRIGGERED,
                    timestampMs = event.timestampMs
                )
            }
            else -> Unit
        }
    }

    suspend fun observeEventsAndApplyTransitions(eventBus: EventBus) {
        eventBus.observe().collect { event ->
            onEvent(event)
        }
    }

    private fun handlePersonSeenEvent(event: EventEnvelope) {
        val personSeen = PersonSeenEventPayload.fromJson(event.payloadJson) ?: return
        if (personSeen.isOwner) {
            transitionTo(
                targetState = RobotBehaviorState.ATTENTIVE,
                reason = BehaviorTransitionReason.OWNER_SEEN,
                timestampMs = personSeen.seenAtMs
            )
        } else {
            transitionTo(
                targetState = RobotBehaviorState.ATTENTIVE,
                reason = BehaviorTransitionReason.NON_OWNER_SEEN,
                timestampMs = personSeen.seenAtMs
            )
        }
    }
}
