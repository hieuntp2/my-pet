package com.aipet.brain.app.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.aipet.brain.ui.avatar.pixel.bridge.PixelPetAvatarIntent

/**
 * Manages temporary one-shot reaction overlays on top of the bridge-driven visual state.
 *
 * When a reaction is active, [effectiveIntent] returns the reaction intent.
 * The caller is responsible for notifying [onReactionClipCompleted] when the one-shot
 * clip finishes, which clears the reaction and reverts to the base-driven state.
 *
 * Priority rules:
 *   - Higher priority reaction always wins over a lower one already active.
 *   - Equal priority reaction does NOT replace an active one (prevents mid-reaction restarts).
 *   - On [onReactionClipCompleted], active reaction is cleared unconditionally.
 *
 * Anti-spam:
 *   - Three taps within [TAP_STREAK_WINDOW_MS] escalates to SURPRISED reaction.
 *   - Self-trigger guard: if last audio output was within [AUDIO_SELF_TRIGGER_GUARD_MS],
 *     sound reactions are suppressed.
 */
@Stable
class PetReactionController {

    var activeReaction by mutableStateOf<PetReactionEntry?>(null)
        private set

    private var lastTapMs = 0L
    private var tapStreak = 0
    private var lastAudioOutputMs = 0L

    // ── Public API ───────────────────────────────────────────────────────────

    /** Called when user taps the pet face. Triggers ENGAGED or SURPRISED (anti-spam). */
    fun triggerTap(isBlocked: Boolean, nowMs: Long = System.currentTimeMillis()) {
        tapStreak = if (nowMs - lastTapMs < TAP_STREAK_WINDOW_MS) tapStreak + 1 else 1
        lastTapMs = nowMs
        val intent = when {
            isBlocked -> PixelPetAvatarIntent.ANNOYED
            tapStreak > TAP_SPAM_THRESHOLD -> PixelPetAvatarIntent.ANNOYED
            else -> PixelPetAvatarIntent.ENGAGED
        }
        setReaction(intent = intent, priority = Priority.INTERACTION)
    }

    /** Called when user long-presses the pet. Triggers LONG_PRESS (warm cuddle reaction). */
    fun triggerLongPress(nowMs: Long = System.currentTimeMillis()) {
        tapStreak = 1
        lastTapMs = nowMs
        setReaction(intent = PixelPetAvatarIntent.LONG_PRESS, priority = Priority.INTERACTION)
    }

    /**
     * Called when app-open greeting is detected.
     * [excited] = true → EXCITED animation; false → ENGAGED (soft greeting).
     */
    fun triggerGreeting(excited: Boolean) {
        val intent = if (excited) PixelPetAvatarIntent.EXCITED else PixelPetAvatarIntent.ENGAGED
        setReaction(intent = intent, priority = Priority.GREETING)
    }

    /**
     * Called on keyword detection (pet heard its name / a command).
     * Uses ATTENTIVE: curious snap-to-attention, not a full surprise.
     */
    fun triggerKeyword() {
        if (isAudioSelfTriggered()) return
        setReaction(intent = PixelPetAvatarIntent.ATTENTIVE, priority = Priority.AUDIO)
    }

    /**
     * Called on loud-sound stimulus.
     * Uses SURPRISED: startle reaction.
     */
    fun triggerLoudSound() {
        if (isAudioSelfTriggered()) return
        setReaction(intent = PixelPetAvatarIntent.SURPRISED, priority = Priority.AUDIO)
    }

    /** Mini-game win: burst celebration reaction. */
    fun triggerGameCelebrate() {
        setReaction(intent = PixelPetAvatarIntent.GAME_CELEBRATE, priority = Priority.GREETING)
    }

    /** Mini-game fail: brief deflated reaction. */
    fun triggerGameFail() {
        setReaction(intent = PixelPetAvatarIntent.GAME_FAIL, priority = Priority.INTERACTION)
    }

    /**
     * Record that the pet just played audio output. Suppress sound-reactive FX for the
     * guard window to prevent the pet from reacting to its own voice.
     */
    fun recordAudioOutput(nowMs: Long = System.currentTimeMillis()) {
        lastAudioOutputMs = nowMs
    }

    /**
     * Called by [HomePixelPetAvatar] when the active reaction clip finishes (isFinished).
     * Clears the reaction so the base intent resumes.
     */
    fun onReactionClipCompleted() {
        activeReaction = null
    }

    /** Returns the intent to use: reaction intent when active, else the base intent. */
    fun effectiveIntent(baseIntent: PixelPetAvatarIntent): PixelPetAvatarIntent =
        activeReaction?.intent ?: baseIntent

    /** Human-readable summary for debug. */
    val debugSummary: String
        get() {
            val r = activeReaction ?: return "no_reaction"
            return "reaction=${r.intent.name.lowercase()} priority=${r.priority} token=${r.token}"
        }

    // ── Internal ─────────────────────────────────────────────────────────────

    private fun setReaction(intent: PixelPetAvatarIntent, priority: Int) {
        val current = activeReaction
        if (current != null && current.priority >= priority) return
        activeReaction = PetReactionEntry(
            intent = intent,
            priority = priority,
            token = System.currentTimeMillis()
        )
    }

    private fun isAudioSelfTriggered(nowMs: Long = System.currentTimeMillis()): Boolean =
        nowMs - lastAudioOutputMs < AUDIO_SELF_TRIGGER_GUARD_MS

    companion object {
        private const val TAP_STREAK_WINDOW_MS = 500L
        private const val TAP_SPAM_THRESHOLD = 3
        private const val AUDIO_SELF_TRIGGER_GUARD_MS = 800L
    }

    object Priority {
        const val GREETING = 3
        const val INTERACTION = 2
        const val AUDIO = 1
    }
}

data class PetReactionEntry(
    val intent: PixelPetAvatarIntent,
    val priority: Int,
    val token: Long
)
