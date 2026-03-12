package com.aipet.brain.brain.state

import android.util.Log
import com.aipet.brain.brain.events.BrainStateChangedEventPayload
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class BrainStateSnapshot(
    val currentState: BrainState,
    val updatedAtMs: Long
)

class BrainStateStore(
    initialState: BrainState = BrainState.IDLE,
    private val nowProvider: () -> Long = { System.currentTimeMillis() },
    private val eventBus: EventBus? = null
) {
    private val state = MutableStateFlow(
        BrainStateSnapshot(
            currentState = initialState,
            updatedAtMs = nowProvider()
        )
    )
    private val stateUpdateLock = Mutex()

    fun currentSnapshot(): BrainStateSnapshot {
        return state.value
    }

    fun observe(): StateFlow<BrainStateSnapshot> {
        return state.asStateFlow()
    }

    suspend fun setState(
        targetState: BrainState,
        timestampMs: Long = nowProvider(),
        reason: String = DEFAULT_CHANGE_REASON
    ): Boolean {
        val normalizedReason = reason.takeIf { it.isNotBlank() } ?: DEFAULT_CHANGE_REASON
        val transition = stateUpdateLock.withLock {
            val current = state.value
            if (current.currentState == targetState) {
                return@withLock null
            }
            state.value = BrainStateSnapshot(
                currentState = targetState,
                updatedAtMs = timestampMs
            )
            BrainStateTransition(
                previousState = current.currentState,
                newState = targetState,
                changedAtMs = timestampMs,
                reason = normalizedReason
            )
        } ?: return false

        publishStateChangedEvent(transition)
        return true
    }

    private suspend fun publishStateChangedEvent(transition: BrainStateTransition) {
        val bus = eventBus ?: return
        try {
            bus.publish(
                EventEnvelope.create(
                    type = EventType.BRAIN_STATE_CHANGED,
                    payloadJson = BrainStateChangedEventPayload(
                        fromState = transition.previousState,
                        toState = transition.newState,
                        reason = transition.reason,
                        changedAtMs = transition.changedAtMs
                    ).toJson(),
                    timestampMs = transition.changedAtMs
                )
            )
        } catch (error: Throwable) {
            Log.e(
                TAG,
                "Failed to publish ${EventType.BRAIN_STATE_CHANGED.name}. " +
                    "from=${transition.previousState.name}, to=${transition.newState.name}, reason=${transition.reason}",
                error
            )
        }
    }

    private data class BrainStateTransition(
        val previousState: BrainState,
        val newState: BrainState,
        val changedAtMs: Long,
        val reason: String
    )

    companion object {
        private const val TAG = "BrainStateStore"
        private const val DEFAULT_CHANGE_REASON = "UNSPECIFIED"
    }
}
