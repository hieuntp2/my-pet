package com.aipet.brain.brain.events

import com.aipet.brain.brain.state.BrainState

data class BrainStateChangedEventPayload(
    val fromState: BrainState,
    val toState: BrainState,
    val reason: String,
    val changedAtMs: Long
) {
    fun toJson(): String {
        return buildString(capacity = 128) {
            append("{")
            append("\"fromState\":\"").append(fromState.name.toJsonEscaped()).append("\",")
            append("\"toState\":\"").append(toState.name.toJsonEscaped()).append("\",")
            append("\"reason\":\"").append(reason.toJsonEscaped()).append("\",")
            append("\"changedAtMs\":").append(changedAtMs)
            append("}")
        }
    }
}
