package com.aipet.brain.brain.fusion

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interprets raw touch events (tap/long-press) into semantic [TouchContext].
 *
 * Tracks:
 * - Affection likelihood (soft single tap or long press vs rapid spam)
 * - Spam detection via cadence window
 */
class TouchInterpreter(
    private val tapWindowMs: Long = TAP_WINDOW_MS,
    private val spamTapThreshold: Int = SPAM_TAP_THRESHOLD,
    private val tapFreshnessMs: Long = TAP_FRESHNESS_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    private val recentTaps = ArrayDeque<Long>()

    @Volatile private var lastLongPressMs: Long = 0L
    @Volatile private var lastTapMs: Long = 0L

    suspend fun processTap(nowMs: Long = nowProvider()): TouchContext {
        return mutex.withLock {
            lastTapMs = nowMs
            // Add to recent taps window
            recentTaps.addLast(nowMs)
            pruneOldTaps(nowMs)
            buildContext(nowMs)
        }
    }

    suspend fun processLongPress(nowMs: Long = nowProvider()): TouchContext {
        return mutex.withLock {
            lastLongPressMs = nowMs
            lastTapMs = nowMs
            recentTaps.clear()
            buildContext(nowMs)
        }
    }

    fun buildCurrentContext(nowMs: Long = nowProvider()): TouchContext {
        pruneOldTaps(nowMs)
        return buildContext(nowMs)
    }

    private fun pruneOldTaps(nowMs: Long) {
        while (recentTaps.isNotEmpty() && (nowMs - recentTaps.first()) > tapWindowMs) {
            recentTaps.removeFirst()
        }
    }

    private fun buildContext(nowMs: Long): TouchContext {
        val tapCount = recentTaps.size
        val tapFresh = lastTapMs > 0L && (nowMs - lastTapMs) < tapFreshnessMs
        val longPressFresh = lastLongPressMs > 0L && (nowMs - lastLongPressMs) < TAP_FRESHNESS_MS * 2

        val isSpammy = tapCount >= spamTapThreshold
        val isAffectionate = longPressFresh || (tapFresh && !isSpammy && tapCount <= 2)

        val affectionLikelihood = when {
            longPressFresh -> 0.9f
            isAffectionate -> 0.7f
            tapFresh && tapCount == 1 -> 0.5f
            else -> 0f
        }
        val spamLikelihood = when {
            isSpammy -> (tapCount.toFloat() / (spamTapThreshold * 2f)).coerceAtMost(1f)
            else -> 0f
        }

        return TouchContext(
            recentTap = tapFresh,
            recentLongPress = longPressFresh,
            affectionLikelihood = affectionLikelihood,
            spamLikelihood = spamLikelihood,
            lastTouchMs = maxOf(lastTapMs, lastLongPressMs),
            recentTapCount = tapCount,
            updatedAtMs = nowMs
        )
    }

    companion object {
        private const val TAP_WINDOW_MS = 3_000L
        private const val SPAM_TAP_THRESHOLD = 5
        private const val TAP_FRESHNESS_MS = 1_500L
    }
}
