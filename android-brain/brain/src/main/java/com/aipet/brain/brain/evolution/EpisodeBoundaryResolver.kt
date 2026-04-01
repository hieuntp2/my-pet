package com.aipet.brain.brain.evolution

/**
 * Session boundary rules for episode grouping.
 * Episodes are closed when the user is inactive for longer than the inactivity timeout,
 * or when the app is backgrounded.
 */
object EpisodeBoundaryResolver {

    /** A session is considered over after this much inactivity. */
    const val INACTIVITY_TIMEOUT_MS: Long = 3 * 60 * 1000L // 3 minutes

    /** Minimum episode duration to be saved — very short accidental opens are skipped. */
    const val MIN_EPISODE_DURATION_MS: Long = 15_000L // 15 seconds

    /** Two bursts within this window can be merged into a single episode. */
    const val BURST_MERGE_WINDOW_MS: Long = 5 * 60 * 1000L // 5 minutes

    /**
     * Returns true if a new activity at [nowMs] should still belong to the open candidate
     * (i.e., the gap is small enough to stay in the same session).
     */
    fun shouldMerge(candidate: EpisodeCandidate, nowMs: Long): Boolean {
        val gapMs = nowMs - candidate.lastActivityMs
        return gapMs <= INACTIVITY_TIMEOUT_MS
    }

    /**
     * Returns true if the candidate has enough data and duration to be worth saving.
     */
    fun isWorthSaving(candidate: EpisodeCandidate, nowMs: Long): Boolean {
        val duration = nowMs - candidate.startTimeMs
        return duration >= MIN_EPISODE_DURATION_MS
    }
}
