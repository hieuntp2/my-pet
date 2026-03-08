package com.aipet.brain.brain.state

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class BrainStateSnapshot(
    val currentState: BrainState,
    val updatedAtMs: Long
)

class BrainStateStore(
    initialState: BrainState = BrainState.IDLE,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val state = MutableStateFlow(
        BrainStateSnapshot(
            currentState = initialState,
            updatedAtMs = nowProvider()
        )
    )

    fun currentSnapshot(): BrainStateSnapshot {
        return state.value
    }

    fun observe(): StateFlow<BrainStateSnapshot> {
        return state.asStateFlow()
    }

    fun setState(
        targetState: BrainState,
        timestampMs: Long = nowProvider()
    ): Boolean {
        val current = state.value
        if (current.currentState == targetState) {
            return false
        }
        state.value = BrainStateSnapshot(
            currentState = targetState,
            updatedAtMs = timestampMs
        )
        return true
    }
}
