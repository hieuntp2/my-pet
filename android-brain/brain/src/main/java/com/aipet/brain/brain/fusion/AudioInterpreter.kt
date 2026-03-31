package com.aipet.brain.brain.fusion

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interprets raw audio signals into a stable [AudioContext].
 *
 * Handles:
 * - Self-playback suppression (pet's own audio should not trigger reaction loops)
 * - Loud sound detection with recovery window
 * - External vs self-generated sound confidence
 */
class AudioInterpreter(
    private val loudSoundThreshold: Float = LOUD_SOUND_THRESHOLD,
    private val loudSoundRecoveryMs: Long = LOUD_SOUND_RECOVERY_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    @Volatile private var selfPlaybackActive: Boolean = false
    @Volatile private var loudSoundStartMs: Long = 0L
    @Volatile private var lastAmbientLevel: Float = 0f

    /** Notify interpreter that the pet is starting/stopping its own audio playback. */
    suspend fun setSelfPlaybackActive(active: Boolean, nowMs: Long = nowProvider()) {
        mutex.withLock {
            selfPlaybackActive = active
            loudSoundStartMs = if (active) nowMs else loudSoundStartMs
        }
    }

    /** Process an audio energy update from the audio capture pipeline. */
    suspend fun processAudioEnergy(
        ambientLevel: Float,
        peakLevel: Float,
        nowMs: Long = nowProvider()
    ): AudioContext {
        return mutex.withLock {
            lastAmbientLevel = ambientLevel

            // Detect loud event
            val loudNow = peakLevel > loudSoundThreshold && !selfPlaybackActive
            if (loudNow && loudSoundStartMs == 0L) {
                loudSoundStartMs = nowMs
            }

            // Recovery: loud event clears after recovery window
            val loudEventActive = loudSoundStartMs > 0L &&
                (nowMs - loudSoundStartMs) < loudSoundRecoveryMs

            if (!loudNow && loudSoundStartMs > 0L &&
                (nowMs - loudSoundStartMs) >= loudSoundRecoveryMs) {
                loudSoundStartMs = 0L
            }

            // External sound confidence: lower when self-playback is active
            val externalSoundConfidence = when {
                selfPlaybackActive -> (ambientLevel * 0.3f).coerceIn(0f, 1f)
                loudEventActive -> 0.9f
                else -> (ambientLevel * 0.7f).coerceIn(0f, 1f)
            }

            val shouldOrient = !selfPlaybackActive &&
                externalSoundConfidence > ORIENT_THRESHOLD &&
                ambientLevel > AMBIENT_WORTH_THRESHOLD

            AudioContext(
                ambientLevel = ambientLevel.coerceIn(0f, 1f),
                loudEventActive = loudEventActive,
                externalSoundConfidence = externalSoundConfidence,
                selfPlaybackActive = selfPlaybackActive,
                shouldOrientToSound = shouldOrient,
                loudSoundStartMs = if (loudEventActive) loudSoundStartMs else 0L,
                updatedAtMs = nowMs
            )
        }
    }

    companion object {
        private const val LOUD_SOUND_THRESHOLD = 0.65f
        private const val LOUD_SOUND_RECOVERY_MS = 4_000L
        private const val ORIENT_THRESHOLD = 0.4f
        private const val AMBIENT_WORTH_THRESHOLD = 0.2f
    }
}
