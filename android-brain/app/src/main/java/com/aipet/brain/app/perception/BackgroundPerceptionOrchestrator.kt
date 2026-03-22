package com.aipet.brain.app.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonUnknownEventPayload
import com.aipet.brain.brain.events.UnknownObjectDetectedPayload
import com.aipet.brain.brain.recognition.PersonRecognitionService
import com.aipet.brain.brain.recognition.RecognitionDecisionEventPublisher
import com.aipet.brain.brain.recognition.RecognitionMemoryStatsUpdater
import com.aipet.brain.memory.objects.ObjectRepository
import com.aipet.brain.app.perception.BackgroundPerceptionController
import com.aipet.brain.perception.vision.face.embedding.FaceEmbeddingEngine
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import com.aipet.brain.perception.vision.objectdetection.ObjectDetectionEngine
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates the background camera pipeline:
 * - Bridges BackgroundPerceptionController callbacks to the EventBus.
 * - Runs face recognition on live face crops → publishes PERSON_RECOGNIZED or emits
 *   unknown-face notifications via [onUnknownFaceDetected].
 * - Checks object detections against ObjectRepository → publishes OBJECT_DETECTED or
 *   emits unknown-object notifications via [onUnknownObjectDetected].
 * - Boosts the camera scan rate when OWNER_SEEN_DETECTED fires on the EventBus.
 *
 * Call [start] after camera permission is confirmed. Call [release] when the composable
 * is disposed.
 */
class BackgroundPerceptionOrchestrator(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope,
    private val eventBus: EventBus,
    private val faceEmbeddingEngine: FaceEmbeddingEngine,
    private val personRecognitionService: PersonRecognitionService,
    private val recognitionDecisionEventPublisher: RecognitionDecisionEventPublisher,
    private val objectDetectionEngine: ObjectDetectionEngine?,
    private val objectRepository: ObjectRepository,
    private val onUnknownFaceDetected: (faceBitmap: Bitmap?, detectedAtMs: Long) -> Unit,
    private val onUnknownObjectDetected: (thumbnail: Bitmap?, canonicalLabel: String, confidence: Float, detectedAtMs: Long) -> Unit
) {

    // Cooldown tracking: avoid spamming dialogs for the same entity.
    private val unknownFaceCooldownUntilMs = java.util.concurrent.atomic.AtomicLong(0L)
    private val unknownObjectCooldowns = java.util.concurrent.ConcurrentHashMap<String, Long>()

    private val controller = BackgroundPerceptionController(
        context = context,
        lifecycleOwner = lifecycleOwner,
        objectDetectionEngine = objectDetectionEngine,
        onFaceDetectionResult = { result -> handleFaceDetectionResult(result) },
        onObjectDetectionResult = { result -> handleObjectDetectionResult(result) },
        onLiveFaceCropReady = { bitmap, ts, rotation -> handleLiveFaceCrop(bitmap, ts, rotation) }
    )

    /** Start background perception (no-op if already started or permission missing). */
    fun start() {
        controller.start()
        observeEventBusForBoosts()
    }

    /** Release camera and stop all processing. */
    fun release() {
        controller.release()
    }

    // ── Event bus observation ──────────────────────────────────────────────────

    private fun observeEventBusForBoosts() {
        eventBus.observe()
            .onEach { event ->
                when (event.type) {
                    EventType.OWNER_SEEN_DETECTED,
                    EventType.PERSON_RECOGNIZED -> controller.boostScanRate()
                    else -> Unit
                }
            }
            .launchIn(coroutineScope)
    }

    // ── Face detection handling ────────────────────────────────────────────────

    private fun handleFaceDetectionResult(result: FaceDetectionResult) {
        // We only post FACE_DETECTED count changes — recognition happens via live face crop.
        if (result.faces.isNotEmpty()) {
            controller.boostScanRate()
        }
    }

    private fun handleLiveFaceCrop(rawBitmap: Bitmap, timestampMs: Long, cameraRotation: Int) {
        coroutineScope.launch(Dispatchers.Default) {
            val bitmap = rotateBitmapForPortrait(rawBitmap, cameraRotation)
            try {
                faceEmbeddingEngine.generateEmbedding(bitmap)
                    .onSuccess { embedding ->
                        val result = personRecognitionService.recognize(embedding)
                        recognitionDecisionEventPublisher.publish(result)

                        val isRecognized = result.accepted && result.bestPersonId != null
                        if (!isRecognized) {
                            maybeEmitUnknownFace(bitmap = bitmap, detectedAtMs = timestampMs)
                        }
                    }
                    .onFailure { error ->
                        Log.w(TAG, "Background face embedding failed: ${error.message}")
                    }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private suspend fun maybeEmitUnknownFace(bitmap: Bitmap, detectedAtMs: Long) {
        val now = System.currentTimeMillis()
        if (now < unknownFaceCooldownUntilMs.get()) return
        unknownFaceCooldownUntilMs.set(now + UNKNOWN_FACE_COOLDOWN_MS)

        // Publish PERSON_UNKNOWN_DETECTED event so the event log has a record.
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN_DETECTED,
                timestampMs = detectedAtMs,
                payloadJson = PersonUnknownEventPayload(
                    seenAtMs = detectedAtMs,
                    source = "background_perception"
                ).toJson()
            )
        )
        // Clone the bitmap for the dialog; the original will be recycled by the caller.
        val snapshot = runCatching { bitmap.copy(bitmap.config ?: Bitmap.Config.ARGB_8888, false) }.getOrNull()
        withContext(Dispatchers.Main) {
            onUnknownFaceDetected(snapshot, detectedAtMs)
        }
        Log.i(TAG, "Unknown face detected — dialog triggered.")
    }

    // ── Object detection handling ──────────────────────────────────────────────

    private fun handleObjectDetectionResult(result: Result<ObjectDetectionResult>) {
        val detectionResult = result.getOrNull() ?: return
        val topDetection = detectionResult.detections.firstOrNull() ?: return
        val label = topDetection.label.trim().ifBlank { return }
        val confidence = topDetection.confidence.coerceIn(0f, 1f)
        val detectedAtMs = detectionResult.timestampMs

        if (confidence < UNKNOWN_OBJECT_MIN_CONFIDENCE) return

        coroutineScope.launch(Dispatchers.IO) {
            val resolvedName = objectRepository.resolveKnownObjectDisplayName(label)
            if (resolvedName != null) {
                // Known object — publish normal OBJECT_DETECTED and skip dialog.
                val seenUpdateResult = runCatching {
                    objectRepository.recordKnownObjectSeen(label = label, seenAtMs = detectedAtMs)
                }.getOrNull()
                eventBus.publish(
                    EventEnvelope.create(
                        type = EventType.OBJECT_DETECTED,
                        timestampMs = detectedAtMs,
                        payloadJson = com.aipet.brain.brain.events.ObjectDetectedEventPayload(
                            objectId = seenUpdateResult?.objectRecord?.objectId,
                            label = label,
                            confidence = confidence,
                            detectedAtMs = detectedAtMs
                        ).toJson()
                    )
                )
                return@launch
            }

            // Unknown object — check cooldown and emit dialog trigger.
            val now = System.currentTimeMillis()
            val cooldownUntil = unknownObjectCooldowns[label] ?: 0L
            if (now < cooldownUntil) return@launch
            unknownObjectCooldowns[label] = now + UNKNOWN_OBJECT_COOLDOWN_MS

            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.UNKNOWN_OBJECT_DETECTED,
                    timestampMs = detectedAtMs,
                    payloadJson = UnknownObjectDetectedPayload(
                        canonicalLabel = label,
                        confidence = confidence,
                        detectedAtMs = detectedAtMs
                    ).toJson()
                )
            )
            // Capture a thumbnail copy from the latest full-frame snapshot.
            val thumbnail = runCatching {
                controller.latestFrameSnapshot?.let { bmp ->
                    bmp.copy(bmp.config ?: Bitmap.Config.ARGB_8888, false)
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                onUnknownObjectDetected(thumbnail, label, confidence, detectedAtMs)
            }
            Log.i(TAG, "Unknown object detected: '$label' (conf=$confidence) — dialog triggered.")
        }
    }

    companion object {
        private const val TAG = "BackgroundPercOrch"

        /** Minimum object detection confidence before triggering an "unknown object" dialog. */
        private const val UNKNOWN_OBJECT_MIN_CONFIDENCE = 0.65f

        /** Cooldown between unknown-face dialogs (ms). */
        private const val UNKNOWN_FACE_COOLDOWN_MS = 60_000L

        /** Cooldown between unknown-object dialogs per label (ms). */
        private const val UNKNOWN_OBJECT_COOLDOWN_MS = 60_000L
    }
}

private fun rotateBitmapForPortrait(bitmap: Bitmap, cameraRotation: Int): Bitmap {
    if (cameraRotation != 90 && cameraRotation != 270) return bitmap
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}
