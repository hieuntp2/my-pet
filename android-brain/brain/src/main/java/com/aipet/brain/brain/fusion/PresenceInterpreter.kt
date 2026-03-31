package com.aipet.brain.brain.fusion

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Interprets raw camera/face detection signals into a stable [PresenceState].
 * Uses temporal smoothing to avoid flickering on single-frame misses.
 */
class PresenceInterpreter(
    private val presenceStabilityWindowMs: Long = PRESENCE_STABILITY_MS,
    private val absenceConfirmationMs: Long = ABSENCE_CONFIRMATION_MS,
    private val nowProvider: () -> Long = { System.currentTimeMillis() }
) {
    private val mutex = Mutex()

    /** When a face was last confidently detected. */
    @Volatile private var lastFaceSeenMs: Long = 0L
    /** When the user first consistently entered the scene. */
    @Volatile private var entryStartMs: Long = 0L
    /** When the user was last confirmed absent. */
    @Volatile private var confirmedAbsentSinceMs: Long = nowProvider()
    /** Whether we're currently tracking the user as present. */
    @Volatile private var trackedPresent: Boolean = false

    @Volatile private var lastRecognizedPersonId: String? = null
    @Volatile private var lastFamiliarPresent: Boolean = false
    @Volatile private var lastFaceCount: Int = 0

    private var entryEventSentMs: Long = 0L
    private var exitEventSentMs: Long = 0L

    suspend fun processFrame(
        faceCount: Int,
        recognizedPersonId: String?,
        faceConfidence: Float,
        nowMs: Long = nowProvider()
    ): PresenceState {
        return mutex.withLock {
            val faceSeen = faceCount > 0 && faceConfidence > MIN_FACE_CONFIDENCE

            if (faceSeen) {
                lastFaceSeenMs = nowMs
                lastFaceCount = faceCount
                if (recognizedPersonId != null) {
                    lastRecognizedPersonId = recognizedPersonId
                    lastFamiliarPresent = true
                }
            }

            val sinceLastFace = nowMs - lastFaceSeenMs
            val wasPresent = trackedPresent

            // Switch to present if we have a recent face
            if (!trackedPresent && sinceLastFace < presenceStabilityWindowMs) {
                trackedPresent = true
                entryStartMs = nowMs
                confirmedAbsentSinceMs = 0L
            }
            // Switch to absent only after confirmed absence window
            if (trackedPresent && sinceLastFace > absenceConfirmationMs) {
                trackedPresent = false
                confirmedAbsentSinceMs = nowMs
                lastFamiliarPresent = false
                lastRecognizedPersonId = null
            }

            val entryEventRecently = trackedPresent && !wasPresent
            val exitEventRecently = !trackedPresent && wasPresent

            val stablePresenceMs = if (trackedPresent && entryStartMs > 0L) {
                nowMs - entryStartMs
            } else 0L

            val absenceMs = if (!trackedPresent && confirmedAbsentSinceMs > 0L) {
                nowMs - confirmedAbsentSinceMs
            } else 0L

            PresenceState(
                userPresent = trackedPresent,
                familiarUserPresent = lastFamiliarPresent && trackedPresent,
                recognizedPersonId = if (trackedPresent) lastRecognizedPersonId else null,
                faceCount = if (trackedPresent) lastFaceCount else 0,
                stablePresenceMs = stablePresenceMs,
                absenceMs = absenceMs,
                entryEventRecently = entryEventRecently,
                exitEventRecently = exitEventRecently,
                updatedAtMs = nowMs
            )
        }
    }

    /** Called when we know the user is associated with a recognized person. */
    suspend fun setRecognizedPerson(personId: String, nowMs: Long = nowProvider()) {
        mutex.withLock {
            lastRecognizedPersonId = personId
            lastFamiliarPresent = true
        }
    }

    companion object {
        private const val PRESENCE_STABILITY_MS = 300L
        private const val ABSENCE_CONFIRMATION_MS = 2_500L
        private const val MIN_FACE_CONFIDENCE = 0.4f
    }
}
