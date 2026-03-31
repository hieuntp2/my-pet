package com.aipet.brain.brain.b2.domain

/**
 * Snapshot of active cooldown timers and fatigue counters used by
 * the intention scoring system to apply penalties and block gates.
 */
data class CooldownState(
    /** When was the last tap reaction played (ms epoch). */
    val lastTouchReactionMs: Long = 0L,
    /** When was the last long-press cuddle played. */
    val lastLongPressCuddleMs: Long = 0L,
    /** When was the last play invitation shown. */
    val lastInvitationMs: Long = 0L,
    /** When did the last game session end. */
    val lastGameEndMs: Long = 0L,
    /** When was the last audio reaction played. */
    val lastAudioReactionMs: Long = 0L,
    /** When was the last loud-sound startle played. */
    val lastLoudSoundMs: Long = 0L,
    /** When was the last attention-seeking behavior shown. */
    val lastSeekAttentionMs: Long = 0L,
    /** When was the last talk bubble shown. */
    val lastBubbleMs: Long = 0L,
    /** How many consecutive play invitations were ignored. */
    val invitationIgnoredStreak: Int = 0,
    /** How many taps occurred in the last recent window. */
    val recentTapCount: Int = 0,
    /** How many consecutive voice commands were sent in the same window. */
    val recentVoiceCommandCount: Int = 0,
    val updatedAtMs: Long = 0L
) {
    /**
     * Returns true if [domainKey] is still within its cooldown window.
     * @param nowMs current epoch time in ms
     */
    fun isOnCooldown(domainKey: CooldownDomain, nowMs: Long): Boolean {
        val lastMs = lastMs(domainKey)
        val windowMs = domainKey.windowMs
        return (nowMs - lastMs) < windowMs
    }

    /** Remaining cooldown ms for [domainKey], 0 if not on cooldown. */
    fun remainingMs(domainKey: CooldownDomain, nowMs: Long): Long {
        val lastMs = lastMs(domainKey)
        val elapsed = nowMs - lastMs
        return (domainKey.windowMs - elapsed).coerceAtLeast(0L)
    }

    private fun lastMs(domain: CooldownDomain): Long = when (domain) {
        CooldownDomain.TOUCH_REACTION -> lastTouchReactionMs
        CooldownDomain.LONG_PRESS_CUDDLE -> lastLongPressCuddleMs
        CooldownDomain.INVITATION -> lastInvitationMs
        CooldownDomain.GAME_FATIGUE -> lastGameEndMs
        CooldownDomain.AUDIO_REACTION -> lastAudioReactionMs
        CooldownDomain.LOUD_SOUND -> lastLoudSoundMs
        CooldownDomain.SEEK_ATTENTION -> lastSeekAttentionMs
        CooldownDomain.BUBBLE -> lastBubbleMs
    }

    companion object {
        val DEFAULT = CooldownState()
    }
}

/**
 * Enumeration of trackable cooldown domains with their default window sizes.
 */
enum class CooldownDomain(val windowMs: Long) {
    TOUCH_REACTION(800L),
    LONG_PRESS_CUDDLE(3_000L),
    INVITATION(30_000L),
    GAME_FATIGUE(60_000L),
    AUDIO_REACTION(1_500L),
    LOUD_SOUND(5_000L),
    SEEK_ATTENTION(15_000L),
    BUBBLE(4_000L)
}
