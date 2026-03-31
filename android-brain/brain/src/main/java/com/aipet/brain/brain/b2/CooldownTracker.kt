package com.aipet.brain.brain.b2

import com.aipet.brain.brain.b2.domain.AfterEffectType
import com.aipet.brain.brain.b2.domain.BehaviorAfterEffect
import com.aipet.brain.brain.b2.domain.BehaviorPlan
import com.aipet.brain.brain.b2.domain.CooldownState
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Manages cooldown timers and fatigue counters for the Behavior Engine.
 * Thread-safe via Mutex. Updates are applied from BehaviorPlan after-effects.
 */
class CooldownTracker(
    initialState: CooldownState = CooldownState.DEFAULT
) {
    private val mutex = Mutex()
    @Volatile
    private var state: CooldownState = initialState

    fun current(): CooldownState = state

    /**
     * Apply after-effects from a completed or started [plan] to update cooldown state.
     */
    suspend fun applyPlanAfterEffects(plan: BehaviorPlan, nowMs: Long) {
        mutex.withLock {
            var next = state
            for (effect in plan.afterEffects) {
                next = applyEffect(next, effect, nowMs)
            }
            state = next.copy(updatedAtMs = nowMs)
        }
    }

    /** Record a tap/long-press touch event. */
    suspend fun recordTouch(isLongPress: Boolean, nowMs: Long, recentTapCount: Int = 0) {
        mutex.withLock {
            state = if (isLongPress) {
                state.copy(
                    lastTouchReactionMs = nowMs,
                    lastLongPressCuddleMs = nowMs,
                    recentTapCount = 0,
                    updatedAtMs = nowMs
                )
            } else {
                state.copy(
                    lastTouchReactionMs = nowMs,
                    recentTapCount = (state.recentTapCount + 1).coerceAtMost(20),
                    updatedAtMs = nowMs
                )
            }
        }
    }

    /** Record a loud sound event. */
    suspend fun recordLoudSound(nowMs: Long) {
        mutex.withLock {
            state = state.copy(lastLoudSoundMs = nowMs, updatedAtMs = nowMs)
        }
    }

    /** Record an audio reaction. */
    suspend fun recordAudioReaction(nowMs: Long) {
        mutex.withLock {
            state = state.copy(lastAudioReactionMs = nowMs, updatedAtMs = nowMs)
        }
    }

    /** Record a voice command. */
    suspend fun recordVoiceCommand(nowMs: Long) {
        mutex.withLock {
            state = state.copy(
                recentVoiceCommandCount = (state.recentVoiceCommandCount + 1).coerceAtMost(10),
                updatedAtMs = nowMs
            )
        }
    }

    /** Record play invitation was shown. */
    suspend fun recordInvitation(nowMs: Long) {
        mutex.withLock {
            state = state.copy(lastInvitationMs = nowMs, updatedAtMs = nowMs)
        }
    }

    /** Record invitation result — accepted resets streak, ignored increments. */
    suspend fun recordInvitationOutcome(accepted: Boolean, nowMs: Long) {
        mutex.withLock {
            state = state.copy(
                invitationIgnoredStreak = if (accepted) 0 else (state.invitationIgnoredStreak + 1).coerceAtMost(5),
                updatedAtMs = nowMs
            )
        }
    }

    /** Reset recent tap count (call when tap window expires). */
    suspend fun resetRecentTapCount(nowMs: Long) {
        mutex.withLock {
            if (state.recentTapCount > 0) {
                state = state.copy(recentTapCount = 0, updatedAtMs = nowMs)
            }
        }
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private fun applyEffect(
        cd: CooldownState,
        effect: BehaviorAfterEffect,
        nowMs: Long
    ): CooldownState = when (effect.type) {
        AfterEffectType.RECORD_TOUCH_COOLDOWN ->
            cd.copy(lastTouchReactionMs = nowMs)
        AfterEffectType.RECORD_LONG_PRESS_COOLDOWN ->
            cd.copy(lastLongPressCuddleMs = nowMs)
        AfterEffectType.RECORD_INVITATION_OUTCOME ->
            cd.copy(lastInvitationMs = nowMs)
        AfterEffectType.RECORD_BUBBLE_COOLDOWN ->
            cd.copy(lastBubbleMs = nowMs)
        AfterEffectType.RECORD_SEEK_ATTENTION_COOLDOWN ->
            cd.copy(lastSeekAttentionMs = nowMs)
        AfterEffectType.INCREMENT_INVITATION_IGNORED ->
            cd.copy(invitationIgnoredStreak = (cd.invitationIgnoredStreak + 1).coerceAtMost(5))
        AfterEffectType.RESET_INVITATION_IGNORED ->
            cd.copy(invitationIgnoredStreak = 0)
        else -> cd
    }
}
