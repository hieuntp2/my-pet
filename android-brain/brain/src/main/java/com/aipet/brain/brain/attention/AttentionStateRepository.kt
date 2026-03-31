package com.aipet.brain.brain.attention

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Holds and exposes the current [AttentionState].
 * Thread-safe via StateFlow; updates are performed atomically by the engine.
 */
class AttentionStateRepository(
    initial: AttentionState = AttentionState.DEFAULT
) {
    private val _state = MutableStateFlow(initial)

    fun observe(): StateFlow<AttentionState> = _state.asStateFlow()

    fun current(): AttentionState = _state.value

    fun update(newState: AttentionState) {
        _state.value = newState
    }
}
