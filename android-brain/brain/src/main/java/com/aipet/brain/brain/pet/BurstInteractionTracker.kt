package com.aipet.brain.brain.pet

/**
 * Tracks rapid interaction bursts within a short time window.
 * Used to apply diminishing returns and detect overstimulation.
 * Ephemeral — lives in the session, not persisted.
 */
class BurstInteractionTracker {

    private val timestamps = ArrayDeque<Long>(16)

    /**
     * Records an interaction at [now] and returns the current burst count
     * within the burst window.
     */
    fun record(now: Long): Int {
        // Purge timestamps older than the burst window
        val windowStart = now - PetEmotionalConfig.BURST_WINDOW_MS
        while (timestamps.isNotEmpty() && timestamps.first() < windowStart) {
            timestamps.removeFirst()
        }
        timestamps.addLast(now)
        return timestamps.size
    }

    /**
     * Peek at the current burst count without adding a new entry.
     */
    fun currentBurstCount(now: Long): Int {
        val windowStart = now - PetEmotionalConfig.BURST_WINDOW_MS
        return timestamps.count { it >= windowStart }
    }

    fun reset() {
        timestamps.clear()
    }
}
