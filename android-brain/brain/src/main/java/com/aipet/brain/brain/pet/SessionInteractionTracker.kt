package com.aipet.brain.brain.pet

/**
 * Tracks the live session interaction state.
 * Lives in app memory only — not persisted across sessions.
 * Consumed by invitation eligibility, scoring, and debug surfaces.
 */
class SessionInteractionTracker {

    private var _state = SessionInteractionState()
    val state: SessionInteractionState get() = _state

    fun recordInteraction() {
        _state = _state.copy(
            sessionInteractionCount = _state.sessionInteractionCount + 1
        )
    }

    fun recordInvitationEmitted(now: Long) {
        _state = _state.copy(
            invitationCount = _state.invitationCount + 1,
            lastInvitationAtMs = now
        )
    }

    fun recordInvitationIgnored() {
        _state = _state.copy(
            ignoredInvitationCount = _state.ignoredInvitationCount + 1
        )
    }

    fun recordInvitationAccepted() {
        // Accepted invitation resets the ignored streak
        _state = _state.copy(
            ignoredInvitationCount = maxOf(0, _state.ignoredInvitationCount - 1)
        )
    }

    fun recordComforted() {
        _state = _state.copy(wasComfortedThisSession = true)
    }

    fun resetForNewSession() {
        _state = SessionInteractionState()
    }
}
