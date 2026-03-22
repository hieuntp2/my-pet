package com.aipet.brain.app.perception

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Base64
import android.util.Log
import androidx.lifecycle.LifecycleOwner
import com.aipet.brain.brain.events.CandidatePersonReadyForTeachPayload
import com.aipet.brain.brain.events.EventBus
import com.aipet.brain.brain.events.EventEnvelope
import com.aipet.brain.brain.events.EventType
import com.aipet.brain.brain.events.PersonTeachAutoCapturePayload
import com.aipet.brain.brain.events.PersonUnknownEventPayload
import com.aipet.brain.brain.events.UnknownFaceCandidateLifecyclePayload
import com.aipet.brain.brain.events.UnknownObjectDetectedPayload
import com.aipet.brain.brain.recognition.PersonRecognitionService
import com.aipet.brain.brain.recognition.RecognitionDecisionEventPublisher
import com.aipet.brain.core.common.math.VectorMath
import com.aipet.brain.memory.objects.ObjectRepository
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.memory.unknownfaces.UnknownFaceCandidateRecord
import com.aipet.brain.memory.unknownfaces.UnknownFaceCandidateStatus
import com.aipet.brain.memory.unknownfaces.UnknownFaceCandidateStore
import com.aipet.brain.memory.unknownfaces.UnknownFaceDecision
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import com.aipet.brain.perception.vision.objectdetection.ObjectDetectionEngine
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Orchestrates continuous background perception while keeping unknown-face teaching stateful.
 *
 * Unknown-face prompts are produced from persistent candidate memory, not from one short
 * session alone. The orchestrator aggregates evidence across frames, re-checks known matches,
 * applies candidate-level suppression, and emits event payloads for debug visibility.
 */
class BackgroundPerceptionOrchestrator(
    private val context: Context,
    private val lifecycleOwner: LifecycleOwner,
    private val coroutineScope: CoroutineScope,
    private val eventBus: EventBus,
    private val faceEmbeddingEngine: com.aipet.brain.perception.vision.face.embedding.FaceEmbeddingEngine,
    private val personRecognitionService: PersonRecognitionService,
    private val recognitionDecisionEventPublisher: RecognitionDecisionEventPublisher,
    private val objectDetectionEngine: ObjectDetectionEngine?,
    private val objectRepository: ObjectRepository,
    private val unknownFaceCandidateStore: UnknownFaceCandidateStore,
    private val faceProfileStore: FaceProfileStore,
    private val onUnknownObjectDetected: (
        thumbnail: Bitmap?,
        canonicalLabel: String,
        confidence: Float,
        detectedAtMs: Long
    ) -> Unit
) {

    private val unknownObjectCooldowns = ConcurrentHashMap<String, Long>()
    private val suppressionEventAtByCandidate = ConcurrentHashMap<String, Long>()
    private val objectEvidenceTracker = StableObjectEvidenceTracker()
    private val stateLock = Any()
    private val boostObserverStarted = AtomicBoolean(false)
    private var activeEncounter: UnknownFaceEncounterState? = null
    private var activeTeachSession: AutoTeachSession? = null

    private val controller = BackgroundPerceptionController(
        context = context,
        lifecycleOwner = lifecycleOwner,
        objectDetectionEngine = objectDetectionEngine,
        onFaceDetectionResult = { result -> handleFaceDetectionResult(result) },
        onObjectDetectionResult = { result -> handleObjectDetectionResult(result) },
        onLiveFaceCropReady = { bitmap, ts, rotation -> handleLiveFaceCrop(bitmap, ts, rotation) }
    )

    fun start() {
        controller.start()
        observeEventBusForBoosts()
        coroutineScope.launch(Dispatchers.IO) {
            runCatching {
                val cutoff = System.currentTimeMillis() - RESOLVED_CANDIDATE_RETENTION_MS
                unknownFaceCandidateStore.deleteResolvedOlderThan(cutoff)
            }
        }
    }

    fun release() {
        controller.release()
    }

    fun suppressUnknownFaceCandidate(
        candidateId: String,
        reason: String = "dialog_skipped"
    ) {
        val normalizedCandidateId = candidateId.trim()
        if (normalizedCandidateId.isBlank()) {
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = unknownFaceCandidateStore.getById(normalizedCandidateId) ?: return@launch
            val suppressedUntil = max(
                existing.suppressedUntilMs ?: 0L,
                now + SKIPPED_PROMPT_SUPPRESSION_MS
            )
            val suppressed = existing.copy(
                status = UnknownFaceCandidateStatus.SUPPRESSED,
                suppressedUntilMs = suppressedUntil,
                updatedAtMs = now
            )
            unknownFaceCandidateStore.upsert(suppressed)
            markEncounterPromptHandled(candidateId = normalizedCandidateId)
            publishUnknownCandidateLifecycle(
                type = EventType.UNKNOWN_FACE_CANDIDATE_SUPPRESSED,
                record = suppressed,
                encounterId = currentEncounterId()
            )
            Log.i(
                TAG,
                "Unknown-face candidate suppressed: candidateId=$normalizedCandidateId, " +
                    "reason=$reason, suppressedUntilMs=$suppressedUntil"
            )
        }
    }

    fun resolveUnknownFaceCandidateAndStartAutoCapture(
        candidateId: String,
        personId: String,
        profileId: String,
        seedEmbedding: List<Float>
    ) {
        val normalizedCandidateId = candidateId.trim()
        val normalizedPersonId = personId.trim()
        val normalizedProfileId = profileId.trim()
        if (
            normalizedCandidateId.isBlank() ||
            normalizedPersonId.isBlank() ||
            normalizedProfileId.isBlank()
        ) {
            return
        }
        coroutineScope.launch(Dispatchers.IO) {
            val now = System.currentTimeMillis()
            val existing = unknownFaceCandidateStore.getById(normalizedCandidateId)
            val resolved = if (existing == null) {
                val reference = seedEmbedding.toFiniteList()
                if (reference.isEmpty()) {
                    return@launch
                }
                UnknownFaceCandidateRecord(
                    candidateId = normalizedCandidateId,
                    status = UnknownFaceCandidateStatus.RESOLVED,
                    representativeEmbedding = reference,
                    previewImageBase64 = null,
                    firstSeenAtMs = now,
                    lastSeenAtMs = now,
                    seenFrameCount = 1,
                    seenEncounterCount = 1,
                    averageQualityScore = 0f,
                    lastPromptAtMs = now,
                    suppressedUntilMs = now + ASKED_PROMPT_SUPPRESSION_MS,
                    closestKnownPersonId = null,
                    closestKnownSimilarity = null,
                    lastDecision = UnknownFaceDecision.UNKNOWN,
                    updatedAtMs = now
                )
            } else {
                existing.copy(
                    status = UnknownFaceCandidateStatus.RESOLVED,
                    lastSeenAtMs = now,
                    updatedAtMs = now
                )
            }
            unknownFaceCandidateStore.upsert(resolved)
            publishUnknownCandidateLifecycle(
                type = EventType.UNKNOWN_FACE_CANDIDATE_RESOLVED,
                record = resolved,
                encounterId = currentEncounterId()
            )
            startAutoTeachSession(
                candidateId = normalizedCandidateId,
                personId = normalizedPersonId,
                profileId = normalizedProfileId,
                seedEmbedding = seedEmbedding.ifEmpty {
                    resolved.representativeEmbedding
                }
            )
        }
    }

    private fun observeEventBusForBoosts() {
        if (!boostObserverStarted.compareAndSet(false, true)) {
            return
        }
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

    private fun handleFaceDetectionResult(result: FaceDetectionResult) {
        if (result.faces.isNotEmpty()) {
            controller.boostScanRate()
        }
    }

    private fun handleLiveFaceCrop(
        rawBitmap: Bitmap,
        timestampMs: Long,
        cameraRotation: Int
    ) {
        coroutineScope.launch(Dispatchers.Default) {
            val bitmap = rotateBitmapForPortrait(rawBitmap, cameraRotation)
            try {
                val quality = evaluateFaceQuality(bitmap)
                if (quality.score < MIN_FACE_RECOGNITION_QUALITY_SCORE) {
                    Log.d(TAG, "Skipping face recognition due to low quality=${quality.score}")
                    return@launch
                }
                faceEmbeddingEngine.generateEmbedding(bitmap)
                    .onSuccess { embedding ->
                        processFaceObservation(
                            embedding = embedding,
                            bitmap = bitmap,
                            quality = quality,
                            timestampMs = timestampMs
                        )
                    }
                    .onFailure { error ->
                        Log.w(TAG, "Background face embedding failed: ${error.message}")
                    }
            } finally {
                bitmap.recycle()
            }
        }
    }

    private suspend fun processFaceObservation(
        embedding: FloatArray,
        bitmap: Bitmap,
        quality: FaceQualityScore,
        timestampMs: Long
    ) {
        val recognitionResult = personRecognitionService.recognize(embedding)
        recognitionDecisionEventPublisher.publish(recognitionResult)

        val closestProbe = resolveClosestKnownProbe(
            embedding = embedding,
            recognitionResult = recognitionResult
        )
        maybeCaptureAutoTeachSample(
            embedding = embedding,
            quality = quality,
            timestampMs = timestampMs
        )

        val recognizedPersonId = recognitionResult.bestPersonId
        if (recognitionResult.accepted && !recognizedPersonId.isNullOrBlank()) {
            clearExpiredEncounter(timestampMs)
            return
        }

        handleUnknownFaceObservation(
            embedding = embedding,
            quality = quality,
            timestampMs = timestampMs,
            closestKnownPersonId = closestProbe.closestPersonId,
            closestKnownSimilarity = closestProbe.closestSimilarity,
            previewImageBase64 = bitmap.toPreviewBase64(quality.score)
        )
    }

    private suspend fun resolveClosestKnownProbe(
        embedding: FloatArray,
        recognitionResult: com.aipet.brain.brain.recognition.model.RecognitionResult
    ): ClosestKnownProbe {
        if (!recognitionResult.bestPersonId.isNullOrBlank()) {
            return ClosestKnownProbe(
                closestPersonId = recognitionResult.bestPersonId,
                closestSimilarity = recognitionResult.bestScore.coerceIn(-1f, 1f)
            )
        }
        val probe = personRecognitionService.recognizeWithThreshold(
            currentEmbedding = embedding,
            acceptanceThreshold = -1f
        )
        return ClosestKnownProbe(
            closestPersonId = probe.bestPersonId,
            closestSimilarity = probe.bestScore.coerceIn(-1f, 1f)
        )
    }

    private suspend fun handleUnknownFaceObservation(
        embedding: FloatArray,
        quality: FaceQualityScore,
        timestampMs: Long,
        closestKnownPersonId: String?,
        closestKnownSimilarity: Float,
        previewImageBase64: String?
    ) {
        var snapshot: UnknownFaceEncounterSnapshot? = null
        synchronized(stateLock) {
            val encounter = upsertEncounterLocked(
                embedding = embedding,
                quality = quality,
                timestampMs = timestampMs,
                closestKnownPersonId = closestKnownPersonId,
                closestKnownSimilarity = closestKnownSimilarity,
                previewImageBase64 = previewImageBase64
            )
            if (timestampMs - encounter.lastEvaluatedAtMs >= DECISION_THROTTLE_MS) {
                encounter.lastEvaluatedAtMs = timestampMs
                snapshot = encounter.toSnapshot()
            }
        }
        val encounterSnapshot = snapshot ?: return
        val decision = evaluateEncounterDecision(encounterSnapshot)

        Log.d(
            TAG,
            "Unknown-face decision: encounter=${encounterSnapshot.encounterId}, " +
                "decision=${decision.decision.name}, sampleCount=${decision.sampleCount}, " +
                "stable=${"%.3f".format(decision.stableScore)}, " +
                "avgQuality=${"%.3f".format(decision.averageQualityScore)}, " +
                "closestKnown=${decision.closestKnownPersonId ?: "-"} " +
                "(${String.format("%.3f", decision.closestKnownSimilarity)})"
        )

        when (decision.decision) {
            UnknownFaceDecision.KNOWN -> Unit
            UnknownFaceDecision.UNCERTAIN -> Unit
            UnknownFaceDecision.UNKNOWN -> {
                handleUnknownDecision(
                    snapshot = encounterSnapshot,
                    decision = decision
                )
            }
        }
    }

    private fun upsertEncounterLocked(
        embedding: FloatArray,
        quality: FaceQualityScore,
        timestampMs: Long,
        closestKnownPersonId: String?,
        closestKnownSimilarity: Float,
        previewImageBase64: String?
    ): UnknownFaceEncounterState {
        val existing = activeEncounter
        val encounter = if (
            existing == null ||
            timestampMs - existing.lastSeenAtMs > ENCOUNTER_BREAK_MS
        ) {
            UnknownFaceEncounterState(
                encounterId = UUID.randomUUID().toString(),
                startedAtMs = timestampMs,
                lastSeenAtMs = timestampMs,
                lastEvaluatedAtMs = 0L,
                samples = mutableListOf(),
                selectedCandidateId = null,
                hasPromptBeenShown = false,
                promptedCandidateId = null,
                bestPreviewBase64 = null,
                bestPreviewQuality = 0f
            )
        } else {
            existing
        }

        encounter.lastSeenAtMs = timestampMs
        encounter.samples.add(
            UnknownFaceSample(
                embedding = embedding.toFiniteArray(),
                qualityScore = quality.score,
                qualityEligibleForPrompt = quality.eligibleForPrompt,
                timestampMs = timestampMs,
                closestKnownPersonId = closestKnownPersonId,
                closestKnownSimilarity = closestKnownSimilarity.coerceIn(-1f, 1f)
            )
        )
        if (encounter.samples.size > MAX_SAMPLES_PER_ENCOUNTER) {
            encounter.samples.removeAt(0)
        }
        if (
            !previewImageBase64.isNullOrBlank() &&
            quality.score >= encounter.bestPreviewQuality
        ) {
            encounter.bestPreviewBase64 = previewImageBase64
            encounter.bestPreviewQuality = quality.score
        }
        activeEncounter = encounter
        return encounter
    }

    private fun evaluateEncounterDecision(
        snapshot: UnknownFaceEncounterSnapshot
    ): UnknownFaceDecisionResult {
        val eligibleSamples = snapshot.samples.filter { sample ->
            sample.qualityEligibleForPrompt && sample.embedding.isNotEmpty()
        }
        val sampleCount = eligibleSamples.size
        val centroid = computeCentroid(
            embeddings = eligibleSamples.map { sample -> sample.embedding }
        ) ?: FloatArray(0)
        if (sampleCount < MIN_ELIGIBLE_SAMPLES_FOR_DECISION || centroid.isEmpty()) {
            return UnknownFaceDecisionResult(
                decision = UnknownFaceDecision.UNCERTAIN,
                sampleCount = sampleCount,
                stableScore = 0f,
                averageQualityScore = eligibleSamples.map { it.qualityScore }.averageAsFloat(),
                centroid = centroid,
                closestKnownPersonId = snapshot.closestKnownPersonId,
                closestKnownSimilarity = snapshot.closestKnownSimilarity
            )
        }

        val stableScore = eligibleSamples
            .map { sample -> cosineSimilarity(sample.embedding, centroid) }
            .averageAsFloat()
        val averageQuality = eligibleSamples
            .map { sample -> sample.qualityScore }
            .averageAsFloat()

        val strictKnownSamples = eligibleSamples.filter { sample ->
            !sample.closestKnownPersonId.isNullOrBlank() &&
                sample.closestKnownSimilarity >= KNOWN_STRICT_SIMILARITY
        }
        val strictKnownVotes = strictKnownSamples
            .groupingBy { sample -> sample.closestKnownPersonId.orEmpty() }
            .eachCount()
        val knownWinner = strictKnownVotes.maxByOrNull { entry -> entry.value }
        val knownStableCount = knownWinner?.value ?: 0
        val knownStableRatio = if (sampleCount == 0) {
            0f
        } else {
            knownStableCount.toFloat() / sampleCount.toFloat()
        }

        if (
            knownWinner != null &&
            knownStableCount >= KNOWN_MIN_STABLE_FRAMES &&
            knownStableRatio >= KNOWN_STABLE_RATIO
        ) {
            return UnknownFaceDecisionResult(
                decision = UnknownFaceDecision.KNOWN,
                sampleCount = sampleCount,
                stableScore = stableScore,
                averageQualityScore = averageQuality,
                centroid = centroid,
                closestKnownPersonId = knownWinner.key,
                closestKnownSimilarity = snapshot.closestKnownSimilarity
            )
        }

        val encounterDurationMs = snapshot.lastSeenAtMs - snapshot.startedAtMs
        val farFromKnown = snapshot.closestKnownSimilarity <= UNKNOWN_MAX_CLOSEST_SIMILARITY
        val stableEnough = stableScore >= READY_MIN_STABILITY
        val qualityEnough = averageQuality >= READY_MIN_AVERAGE_QUALITY
        val enoughSamples = sampleCount >= READY_MIN_ELIGIBLE_SAMPLES
        val enoughDuration = encounterDurationMs >= READY_MIN_DURATION_MS

        if (enoughSamples && enoughDuration && stableEnough && qualityEnough && farFromKnown) {
            return UnknownFaceDecisionResult(
                decision = UnknownFaceDecision.UNKNOWN,
                sampleCount = sampleCount,
                stableScore = stableScore,
                averageQualityScore = averageQuality,
                centroid = centroid,
                closestKnownPersonId = snapshot.closestKnownPersonId,
                closestKnownSimilarity = snapshot.closestKnownSimilarity
            )
        }

        return UnknownFaceDecisionResult(
            decision = UnknownFaceDecision.UNCERTAIN,
            sampleCount = sampleCount,
            stableScore = stableScore,
            averageQualityScore = averageQuality,
            centroid = centroid,
            closestKnownPersonId = snapshot.closestKnownPersonId,
            closestKnownSimilarity = snapshot.closestKnownSimilarity
        )
    }

    private suspend fun handleUnknownDecision(
        snapshot: UnknownFaceEncounterSnapshot,
        decision: UnknownFaceDecisionResult
    ) {
        if (decision.centroid.isEmpty()) {
            return
        }
        if (snapshot.hasPromptBeenShown) {
            return
        }

        val now = System.currentTimeMillis()
        val activeCandidates = unknownFaceCandidateStore.listActive()
        val existing = activeCandidates.findBestCandidateForEmbedding(decision.centroid, now)
        val candidate = if (existing == null) {
            buildNewCandidateRecord(
                snapshot = snapshot,
                decision = decision,
                now = now
            )
        } else {
            mergeCandidateRecord(
                existing = existing,
                snapshot = snapshot,
                decision = decision,
                now = now
            )
        }

        unknownFaceCandidateStore.upsert(candidate)
        setEncounterSelectedCandidate(snapshot.encounterId, candidate.candidateId)
        publishUnknownCandidateLifecycle(
            type = if (existing == null) {
                EventType.UNKNOWN_FACE_CANDIDATE_CREATED
            } else {
                EventType.UNKNOWN_FACE_CANDIDATE_UPDATED
            },
            record = candidate,
            encounterId = snapshot.encounterId
        )

        tryPromptForUnknownCandidate(
            snapshot = snapshot,
            decision = decision,
            candidate = candidate,
            now = now
        )
    }

    private fun buildNewCandidateRecord(
        snapshot: UnknownFaceEncounterSnapshot,
        decision: UnknownFaceDecisionResult,
        now: Long
    ): UnknownFaceCandidateRecord {
        return UnknownFaceCandidateRecord(
            candidateId = UUID.randomUUID().toString(),
            status = UnknownFaceCandidateStatus.COLLECTING,
            representativeEmbedding = decision.centroid.toList(),
            previewImageBase64 = snapshot.bestPreviewBase64,
            firstSeenAtMs = snapshot.startedAtMs,
            lastSeenAtMs = snapshot.lastSeenAtMs,
            seenFrameCount = decision.sampleCount,
            seenEncounterCount = 1,
            averageQualityScore = decision.averageQualityScore,
            lastPromptAtMs = null,
            suppressedUntilMs = null,
            closestKnownPersonId = decision.closestKnownPersonId,
            closestKnownSimilarity = decision.closestKnownSimilarity,
            lastDecision = UnknownFaceDecision.UNKNOWN,
            updatedAtMs = now
        )
    }

    private fun mergeCandidateRecord(
        existing: UnknownFaceCandidateRecord,
        snapshot: UnknownFaceEncounterSnapshot,
        decision: UnknownFaceDecisionResult,
        now: Long
    ): UnknownFaceCandidateRecord {
        val mergedEmbedding = blendEmbeddings(
            existingEmbedding = existing.representativeEmbedding.toFiniteArray(),
            existingWeight = existing.seenFrameCount.coerceAtLeast(1),
            currentEmbedding = decision.centroid,
            currentWeight = decision.sampleCount.coerceAtLeast(1)
        ).toList()
        val updatedFrameCount = existing.seenFrameCount + decision.sampleCount
        val encounterIncrement = if (
            snapshot.startedAtMs - existing.lastSeenAtMs > ENCOUNTER_BREAK_MS
        ) {
            1
        } else {
            0
        }
        val updatedEncounterCount = existing.seenEncounterCount + encounterIncrement
        val updatedAverageQuality = weightedAverage(
            lhs = existing.averageQualityScore,
            lhsWeight = existing.seenFrameCount.coerceAtLeast(0),
            rhs = decision.averageQualityScore,
            rhsWeight = decision.sampleCount.coerceAtLeast(0)
        )
        return existing.copy(
            status = if (
                existing.status == UnknownFaceCandidateStatus.ASKED &&
                (existing.suppressedUntilMs ?: 0L) > now
            ) {
                UnknownFaceCandidateStatus.SUPPRESSED
            } else {
                UnknownFaceCandidateStatus.COLLECTING
            },
            representativeEmbedding = mergedEmbedding,
            previewImageBase64 = snapshot.bestPreviewBase64 ?: existing.previewImageBase64,
            lastSeenAtMs = snapshot.lastSeenAtMs,
            seenFrameCount = updatedFrameCount,
            seenEncounterCount = updatedEncounterCount,
            averageQualityScore = updatedAverageQuality,
            closestKnownPersonId = decision.closestKnownPersonId,
            closestKnownSimilarity = decision.closestKnownSimilarity,
            lastDecision = UnknownFaceDecision.UNKNOWN,
            updatedAtMs = now
        )
    }

    private suspend fun tryPromptForUnknownCandidate(
        snapshot: UnknownFaceEncounterSnapshot,
        decision: UnknownFaceDecisionResult,
        candidate: UnknownFaceCandidateRecord,
        now: Long
    ) {
        if (snapshot.hasPromptBeenShown) {
            return
        }
        if (hasActiveTeachSession(now)) {
            return
        }
        val alreadyPromptedInEncounter = snapshot.promptedCandidateId == candidate.candidateId
        if (alreadyPromptedInEncounter) {
            return
        }
        val suppressionFromLastPrompt = (candidate.lastPromptAtMs ?: 0L) + ASKED_PROMPT_SUPPRESSION_MS
        val suppressionFromStatus = candidate.suppressedUntilMs ?: 0L
        val suppressionUntil = max(suppressionFromLastPrompt, suppressionFromStatus)
        if (now < suppressionUntil) {
            val suppressed = candidate.copy(
                status = UnknownFaceCandidateStatus.SUPPRESSED,
                suppressedUntilMs = suppressionUntil,
                updatedAtMs = now
            )
            unknownFaceCandidateStore.upsert(suppressed)
            setEncounterPromptHandled(
                encounterId = snapshot.encounterId,
                candidateId = candidate.candidateId
            )
            maybePublishSuppressionEvent(
                suppressed = suppressed,
                encounterId = snapshot.encounterId,
                now = now
            )
            return
        }

        val asked = candidate.copy(
            status = UnknownFaceCandidateStatus.ASKED,
            lastPromptAtMs = now,
            suppressedUntilMs = now + ASKED_PROMPT_SUPPRESSION_MS,
            updatedAtMs = now
        )
        unknownFaceCandidateStore.upsert(asked)
        setEncounterPromptHandled(
            encounterId = snapshot.encounterId,
            candidateId = asked.candidateId
        )
        val activeCandidateCount = unknownFaceCandidateStore.countActive()
        publishUnknownCandidateLifecycle(
            type = EventType.UNKNOWN_FACE_CANDIDATE_READY_TO_ASK,
            record = asked,
            encounterId = snapshot.encounterId,
            activeCandidateCountOverride = activeCandidateCount
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_UNKNOWN_DETECTED,
                timestampMs = now,
                payloadJson = PersonUnknownEventPayload(
                    seenAtMs = now,
                    source = "background_unknown_face_candidate"
                ).toJson()
            )
        )
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.CANDIDATE_PERSON_READY_FOR_TEACH,
                timestampMs = now,
                payloadJson = CandidatePersonReadyForTeachPayload(
                    sessionId = snapshot.encounterId,
                    sampleCount = decision.sampleCount,
                    stableScore = decision.stableScore,
                    centroidEmbedding = decision.centroid.toList(),
                    previewImageBase64 = asked.previewImageBase64,
                    readyAtMs = now,
                    candidateId = asked.candidateId,
                    decision = decision.decision.name,
                    seenFrameCount = asked.seenFrameCount,
                    seenEncounterCount = asked.seenEncounterCount,
                    averageQualityScore = asked.averageQualityScore,
                    lastPromptAtMs = asked.lastPromptAtMs,
                    suppressedUntilMs = asked.suppressedUntilMs,
                    closestKnownPersonId = asked.closestKnownPersonId,
                    closestKnownSimilarity = asked.closestKnownSimilarity,
                    activeCandidateCount = activeCandidateCount
                ).toJson()
            )
        )
        Log.i(
            TAG,
            "Unknown-face prompt emitted: candidateId=${asked.candidateId}, " +
                "encounter=${snapshot.encounterId}, sampleCount=${decision.sampleCount}, " +
                "stable=${"%.3f".format(decision.stableScore)}"
        )
    }

    private fun handleObjectDetectionResult(result: Result<ObjectDetectionResult>) {
        val detectionResult = result.getOrNull()
        val topDetection = detectionResult?.detections
            ?.firstOrNull { detection ->
                detection.boundingBox?.isUsableForStableTracking() == true
            }
        val snapshot = objectEvidenceTracker.update(
            detection = topDetection,
            observedAtMs = detectionResult?.timestampMs ?: System.currentTimeMillis()
        )
        if (!snapshot.readyForDecision || snapshot.label == null || snapshot.displayConfidence < UNKNOWN_OBJECT_MIN_CONFIDENCE) {
            return
        }

        val label = snapshot.label
        val confidence = snapshot.displayConfidence.coerceIn(0f, 1f)
        val detectedAtMs = snapshot.timestampMs

        coroutineScope.launch(Dispatchers.IO) {
            val resolvedName = objectRepository.resolveKnownObjectDisplayName(label)
            if (resolvedName != null) {
                val seenUpdateResult = runCatching {
                    objectRepository.recordKnownObjectSeen(
                        label = label,
                        seenAtMs = detectedAtMs
                    )
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

            val now = System.currentTimeMillis()
            val cooldownUntil = unknownObjectCooldowns[label] ?: 0L
            if (now < cooldownUntil) {
                return@launch
            }
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
            val thumbnail = runCatching {
                controller.latestFrameSnapshot?.let { frame ->
                    frame.copy(frame.config ?: Bitmap.Config.ARGB_8888, false)
                }
            }.getOrNull()
            withContext(Dispatchers.Main) {
                onUnknownObjectDetected(thumbnail, label, confidence, detectedAtMs)
            }
        }
    }

    private fun startAutoTeachSession(
        candidateId: String,
        personId: String,
        profileId: String,
        seedEmbedding: List<Float>
    ) {
        val normalizedSeed = seedEmbedding.toFiniteArray()
        if (normalizedSeed.isEmpty()) {
            return
        }
        val now = System.currentTimeMillis()
        val session = AutoTeachSession(
            candidateId = candidateId,
            personId = personId,
            profileId = profileId,
            windowStartAtMs = now,
            windowEndAtMs = now + AUTO_TEACH_WINDOW_MS,
            targetSampleCount = AUTO_TEACH_TARGET_SAMPLES,
            capturedSampleCount = 0,
            referenceEmbedding = VectorMath.l2Normalize(normalizedSeed),
            lastCapturedEmbedding = null,
            lastCapturedAtMs = 0L
        )
        synchronized(stateLock) {
            activeTeachSession = session
            activeEncounter?.let { encounter ->
                encounter.hasPromptBeenShown = true
                encounter.promptedCandidateId = candidateId
                encounter.selectedCandidateId = candidateId
            }
        }
        coroutineScope.launch(Dispatchers.IO) {
            eventBus.publish(
                EventEnvelope.create(
                    type = EventType.PERSON_TEACH_AUTO_CAPTURE_STARTED,
                    timestampMs = now,
                    payloadJson = PersonTeachAutoCapturePayload(
                        candidateId = candidateId,
                        personId = personId,
                        profileId = profileId,
                        windowStartAtMs = session.windowStartAtMs,
                        windowEndAtMs = session.windowEndAtMs,
                        targetSampleCount = session.targetSampleCount,
                        capturedSampleCount = session.capturedSampleCount,
                        status = "STARTED",
                        completedAtMs = null,
                        completionReason = null
                    ).toJson()
                )
            )
            delay(AUTO_TEACH_WINDOW_MS + 250L)
            completeAutoTeachSession(
                reason = "timeout",
                completedAtMs = System.currentTimeMillis()
            )
        }
    }

    private suspend fun maybeCaptureAutoTeachSample(
        embedding: FloatArray,
        quality: FaceQualityScore,
        timestampMs: Long
    ) {
        if (quality.score < AUTO_TEACH_MIN_QUALITY_SCORE) {
            return
        }
        val normalizedEmbedding = VectorMath.l2Normalize(embedding.toFiniteArray())
        if (normalizedEmbedding.isEmpty()) {
            return
        }
        val session = synchronized(stateLock) { activeTeachSession } ?: return
        if (timestampMs > session.windowEndAtMs) {
            completeAutoTeachSession(
                reason = "timeout",
                completedAtMs = timestampMs
            )
            return
        }
        if (timestampMs - session.lastCapturedAtMs < AUTO_TEACH_CAPTURE_MIN_INTERVAL_MS) {
            return
        }
        val similarity = cosineSimilarity(normalizedEmbedding, session.referenceEmbedding)
        if (similarity < AUTO_TEACH_MIN_SIMILARITY) {
            return
        }
        val duplicate = session.lastCapturedEmbedding?.let { previous ->
            cosineSimilarity(previous, normalizedEmbedding) >= AUTO_TEACH_DUPLICATE_SIMILARITY
        } ?: false
        if (duplicate) {
            return
        }
        val persisted = faceProfileStore.addEmbeddingToProfile(
            profileId = session.profileId,
            values = normalizedEmbedding.toList(),
            metadata = "auto_teach_unknown_candidate:${session.candidateId}"
        ) ?: return

        var shouldComplete = false
        synchronized(stateLock) {
            val active = activeTeachSession ?: return@synchronized
            if (
                active.candidateId != session.candidateId ||
                active.profileId != session.profileId
            ) {
                return@synchronized
            }
            active.capturedSampleCount += 1
            active.lastCapturedAtMs = timestampMs
            active.lastCapturedEmbedding = normalizedEmbedding
            active.referenceEmbedding = blendEmbeddings(
                existingEmbedding = active.referenceEmbedding,
                existingWeight = max(1, active.capturedSampleCount),
                currentEmbedding = normalizedEmbedding,
                currentWeight = 1
            )
            shouldComplete = active.capturedSampleCount >= active.targetSampleCount
        }

        Log.d(
            TAG,
            "Auto-teach sample captured: candidateId=${session.candidateId}, " +
                "profileId=${session.profileId}, embeddingId=${persisted.embeddingId}"
        )

        if (shouldComplete) {
            completeAutoTeachSession(
                reason = "target_reached",
                completedAtMs = timestampMs
            )
        }
    }

    private suspend fun completeAutoTeachSession(
        reason: String,
        completedAtMs: Long
    ) {
        val session = synchronized(stateLock) {
            val active = activeTeachSession ?: return
            activeTeachSession = null
            active
        }
        eventBus.publish(
            EventEnvelope.create(
                type = EventType.PERSON_TEACH_AUTO_CAPTURE_COMPLETED,
                timestampMs = completedAtMs,
                payloadJson = PersonTeachAutoCapturePayload(
                    candidateId = session.candidateId,
                    personId = session.personId,
                    profileId = session.profileId,
                    windowStartAtMs = session.windowStartAtMs,
                    windowEndAtMs = session.windowEndAtMs,
                    targetSampleCount = session.targetSampleCount,
                    capturedSampleCount = session.capturedSampleCount,
                    status = "COMPLETED",
                    completedAtMs = completedAtMs,
                    completionReason = reason
                ).toJson()
            )
        )
        Log.i(
            TAG,
            "Auto-teach completed: candidateId=${session.candidateId}, " +
                "captured=${session.capturedSampleCount}, reason=$reason"
        )
    }

    private fun hasActiveTeachSession(now: Long): Boolean {
        val session = synchronized(stateLock) { activeTeachSession } ?: return false
        if (now <= session.windowEndAtMs && session.capturedSampleCount < session.targetSampleCount) {
            return true
        }
        coroutineScope.launch(Dispatchers.IO) {
            completeAutoTeachSession(
                reason = "timeout",
                completedAtMs = now
            )
        }
        return false
    }

    private suspend fun publishUnknownCandidateLifecycle(
        type: EventType,
        record: UnknownFaceCandidateRecord,
        encounterId: String?,
        activeCandidateCountOverride: Int? = null
    ) {
        val activeCandidateCount = activeCandidateCountOverride
            ?: unknownFaceCandidateStore.countActive()
        eventBus.publish(
            EventEnvelope.create(
                type = type,
                timestampMs = System.currentTimeMillis(),
                payloadJson = UnknownFaceCandidateLifecyclePayload(
                    candidateId = record.candidateId,
                    status = record.status.name,
                    decision = record.lastDecision.name,
                    seenFrameCount = record.seenFrameCount,
                    seenEncounterCount = record.seenEncounterCount,
                    averageQualityScore = record.averageQualityScore,
                    activeCandidateCount = activeCandidateCount,
                    eventAtMs = System.currentTimeMillis(),
                    lastPromptAtMs = record.lastPromptAtMs,
                    suppressedUntilMs = record.suppressedUntilMs,
                    closestKnownPersonId = record.closestKnownPersonId,
                    closestKnownSimilarity = record.closestKnownSimilarity,
                    encounterId = encounterId,
                    source = "background_unknown_face_candidate"
                ).toJson()
            )
        )
    }

    private suspend fun maybePublishSuppressionEvent(
        suppressed: UnknownFaceCandidateRecord,
        encounterId: String?,
        now: Long
    ) {
        val lastEmitted = suppressionEventAtByCandidate[suppressed.candidateId] ?: 0L
        if (now - lastEmitted < SUPPRESSION_EVENT_THROTTLE_MS) {
            return
        }
        suppressionEventAtByCandidate[suppressed.candidateId] = now
        publishUnknownCandidateLifecycle(
            type = EventType.UNKNOWN_FACE_CANDIDATE_SUPPRESSED,
            record = suppressed,
            encounterId = encounterId
        )
    }

    private fun setEncounterSelectedCandidate(
        encounterId: String,
        candidateId: String
    ) {
        synchronized(stateLock) {
            val encounter = activeEncounter ?: return
            if (encounter.encounterId != encounterId) {
                return
            }
            encounter.selectedCandidateId = candidateId
        }
    }

    private fun setEncounterPromptHandled(
        encounterId: String,
        candidateId: String
    ) {
        synchronized(stateLock) {
            val encounter = activeEncounter ?: return
            if (encounter.encounterId != encounterId) {
                return
            }
            encounter.hasPromptBeenShown = true
            encounter.promptedCandidateId = candidateId
            encounter.selectedCandidateId = candidateId
        }
    }

    private fun markEncounterPromptHandled(candidateId: String) {
        synchronized(stateLock) {
            val encounter = activeEncounter ?: return
            if (
                encounter.selectedCandidateId == candidateId ||
                encounter.promptedCandidateId == candidateId
            ) {
                encounter.hasPromptBeenShown = true
                encounter.promptedCandidateId = candidateId
            }
        }
    }

    private fun clearExpiredEncounter(timestampMs: Long) {
        synchronized(stateLock) {
            val encounter = activeEncounter ?: return
            if (timestampMs - encounter.lastSeenAtMs > ENCOUNTER_BREAK_MS) {
                activeEncounter = null
            }
        }
    }

    private fun currentEncounterId(): String? {
        return synchronized(stateLock) { activeEncounter?.encounterId }
    }

    companion object {
        private const val TAG = "BackgroundPercOrch"

        private const val UNKNOWN_OBJECT_MIN_CONFIDENCE = 0.72f
        private const val UNKNOWN_OBJECT_COOLDOWN_MS = 60_000L

        private const val ENCOUNTER_BREAK_MS = 2_400L
        private const val DECISION_THROTTLE_MS = 800L
        private const val MAX_SAMPLES_PER_ENCOUNTER = 14
        private const val MIN_ELIGIBLE_SAMPLES_FOR_DECISION = 5
        private const val READY_MIN_ELIGIBLE_SAMPLES = 7
        private const val READY_MIN_DURATION_MS = 2_600L
        private const val READY_MIN_STABILITY = 0.78f
        private const val READY_MIN_AVERAGE_QUALITY = 0.48f
        private const val KNOWN_STRICT_SIMILARITY = 0.80f
        private const val KNOWN_MIN_STABLE_FRAMES = 4
        private const val KNOWN_STABLE_RATIO = 0.70f
        private const val UNKNOWN_MAX_CLOSEST_SIMILARITY = 0.35f

        internal const val CANDIDATE_MERGE_MIN_SIMILARITY = 0.80f
        internal const val CANDIDATE_MERGE_MAX_GAP_MS = 7L * 24L * 60L * 60L * 1000L
        private const val ASKED_PROMPT_SUPPRESSION_MS = 5L * 60L * 1000L
        private const val SKIPPED_PROMPT_SUPPRESSION_MS = 15L * 60L * 1000L
        private const val SUPPRESSION_EVENT_THROTTLE_MS = 5_000L
        private const val RESOLVED_CANDIDATE_RETENTION_MS = 14L * 24L * 60L * 60L * 1000L

        private const val AUTO_TEACH_WINDOW_MS = 15_000L
        private const val AUTO_TEACH_TARGET_SAMPLES = 8
        private const val AUTO_TEACH_MIN_QUALITY_SCORE = 0.45f
        private const val AUTO_TEACH_MIN_SIMILARITY = 0.72f
        private const val AUTO_TEACH_DUPLICATE_SIMILARITY = 0.98f
        private const val AUTO_TEACH_CAPTURE_MIN_INTERVAL_MS = 700L
        private const val MIN_FACE_RECOGNITION_QUALITY_SCORE = 0.28f
    }
}

private data class ClosestKnownProbe(
    val closestPersonId: String?,
    val closestSimilarity: Float
)

private data class UnknownFaceSample(
    val embedding: FloatArray,
    val qualityScore: Float,
    val qualityEligibleForPrompt: Boolean,
    val timestampMs: Long,
    val closestKnownPersonId: String?,
    val closestKnownSimilarity: Float
)

private data class UnknownFaceEncounterState(
    val encounterId: String,
    val startedAtMs: Long,
    var lastSeenAtMs: Long,
    var lastEvaluatedAtMs: Long,
    val samples: MutableList<UnknownFaceSample>,
    var selectedCandidateId: String?,
    var hasPromptBeenShown: Boolean,
    var promptedCandidateId: String?,
    var bestPreviewBase64: String?,
    var bestPreviewQuality: Float
) {
    fun toSnapshot(): UnknownFaceEncounterSnapshot {
        val closest = samples
            .filter { !it.closestKnownPersonId.isNullOrBlank() }
            .maxByOrNull { it.closestKnownSimilarity }
        return UnknownFaceEncounterSnapshot(
            encounterId = encounterId,
            startedAtMs = startedAtMs,
            lastSeenAtMs = lastSeenAtMs,
            samples = samples.toList(),
            selectedCandidateId = selectedCandidateId,
            hasPromptBeenShown = hasPromptBeenShown,
            promptedCandidateId = promptedCandidateId,
            bestPreviewBase64 = bestPreviewBase64,
            closestKnownPersonId = closest?.closestKnownPersonId,
            closestKnownSimilarity = closest?.closestKnownSimilarity ?: 0f
        )
    }
}

private data class UnknownFaceEncounterSnapshot(
    val encounterId: String,
    val startedAtMs: Long,
    val lastSeenAtMs: Long,
    val samples: List<UnknownFaceSample>,
    val selectedCandidateId: String?,
    val hasPromptBeenShown: Boolean,
    val promptedCandidateId: String?,
    val bestPreviewBase64: String?,
    val closestKnownPersonId: String?,
    val closestKnownSimilarity: Float
)

private data class UnknownFaceDecisionResult(
    val decision: UnknownFaceDecision,
    val sampleCount: Int,
    val stableScore: Float,
    val averageQualityScore: Float,
    val centroid: FloatArray,
    val closestKnownPersonId: String?,
    val closestKnownSimilarity: Float
)

private data class AutoTeachSession(
    val candidateId: String,
    val personId: String,
    val profileId: String,
    val windowStartAtMs: Long,
    val windowEndAtMs: Long,
    val targetSampleCount: Int,
    var capturedSampleCount: Int,
    var referenceEmbedding: FloatArray,
    var lastCapturedEmbedding: FloatArray?,
    var lastCapturedAtMs: Long
)

private data class FaceQualityScore(
    val score: Float,
    val eligibleForPrompt: Boolean
)

private fun List<UnknownFaceCandidateRecord>.findBestCandidateForEmbedding(
    embedding: FloatArray,
    now: Long
): UnknownFaceCandidateRecord? {
    var bestCandidate: UnknownFaceCandidateRecord? = null
    var bestSimilarity = Float.NEGATIVE_INFINITY
    for (candidate in this) {
        if (candidate.status == UnknownFaceCandidateStatus.RESOLVED) {
            continue
        }
        if (now - candidate.lastSeenAtMs > BackgroundPerceptionOrchestrator.CANDIDATE_MERGE_MAX_GAP_MS) {
            continue
        }
        val similarity = cosineSimilarity(
            candidate.representativeEmbedding.toFiniteArray(),
            embedding
        )
        if (similarity < BackgroundPerceptionOrchestrator.CANDIDATE_MERGE_MIN_SIMILARITY) {
            continue
        }
        if (bestCandidate == null || similarity > bestSimilarity) {
            bestCandidate = candidate
            bestSimilarity = similarity
        }
    }
    return bestCandidate
}

private fun evaluateFaceQuality(bitmap: Bitmap): FaceQualityScore {
    val minDim = min(bitmap.width, bitmap.height)
    if (minDim < FACE_MIN_DIMENSION_PX) {
        return FaceQualityScore(
            score = 0.05f,
            eligibleForPrompt = false
        )
    }

    val sampleGridWidth = QUALITY_SAMPLE_GRID
    val sampleGridHeight = QUALITY_SAMPLE_GRID
    val pixels = IntArray(sampleGridWidth * sampleGridHeight)
    val scaled = Bitmap.createScaledBitmap(
        bitmap,
        sampleGridWidth,
        sampleGridHeight,
        true
    )
    scaled.getPixels(
        pixels,
        0,
        sampleGridWidth,
        0,
        0,
        sampleGridWidth,
        sampleGridHeight
    )
    scaled.recycle()

    val lumaValues = FloatArray(pixels.size)
    var lumaSum = 0f
    for (index in pixels.indices) {
        val pixel = pixels[index]
        val r = ((pixel shr 16) and 0xFF).toFloat()
        val g = ((pixel shr 8) and 0xFF).toFloat()
        val b = (pixel and 0xFF).toFloat()
        val luma = 0.299f * r + 0.587f * g + 0.114f * b
        lumaValues[index] = luma
        lumaSum += luma
    }
    val lumaMean = if (lumaValues.isEmpty()) 0f else lumaSum / lumaValues.size.toFloat()
    var varianceAccumulator = 0f
    var gradientAccumulator = 0f
    for (y in 0 until sampleGridHeight) {
        for (x in 0 until sampleGridWidth) {
            val index = y * sampleGridWidth + x
            val current = lumaValues[index]
            val delta = current - lumaMean
            varianceAccumulator += delta * delta

            if (x + 1 < sampleGridWidth) {
                gradientAccumulator += kotlin.math.abs(current - lumaValues[index + 1])
            }
            if (y + 1 < sampleGridHeight) {
                gradientAccumulator += kotlin.math.abs(current - lumaValues[index + sampleGridWidth])
            }
        }
    }
    val variance = if (lumaValues.isEmpty()) {
        0f
    } else {
        varianceAccumulator / lumaValues.size.toFloat()
    }
    val gradientNorm = gradientAccumulator /
        (sampleGridWidth * sampleGridHeight * 2f).coerceAtLeast(1f)

    val contrastScore = (variance / 850f).coerceIn(0f, 1f)
    val sharpnessScore = (gradientNorm / 18f).coerceIn(0f, 1f)
    val sizeScore = (
        (minDim - FACE_MIN_DIMENSION_PX).toFloat() /
            (FACE_READY_DIMENSION_PX - FACE_MIN_DIMENSION_PX).toFloat()
        ).coerceIn(0f, 1f)
    val ratio = bitmap.width.toFloat() / bitmap.height.toFloat().coerceAtLeast(1f)
    val ratioScore = if (ratio in FACE_ASPECT_RATIO_MIN..FACE_ASPECT_RATIO_MAX) 1f else 0.35f

    val score = (
        0.40f * contrastScore +
            0.35f * sharpnessScore +
            0.15f * sizeScore +
            0.10f * ratioScore
        ).coerceIn(0f, 1f)
    val eligible = minDim >= FACE_READY_DIMENSION_PX && score >= READY_MIN_QUALITY_SCORE
    return FaceQualityScore(
        score = score,
        eligibleForPrompt = eligible
    )
}

private fun Bitmap.toPreviewBase64(qualityScore: Float): String? {
    if (qualityScore < PREVIEW_CAPTURE_MIN_QUALITY_SCORE) {
        return null
    }
    val previewSize = PREVIEW_MAX_SIZE_PX
    val scale = max(width, height).toFloat() / previewSize.toFloat()
    val scaled = if (scale > 1f) {
        val targetWidth = (width / scale).toInt().coerceAtLeast(1)
        val targetHeight = (height / scale).toInt().coerceAtLeast(1)
        Bitmap.createScaledBitmap(this, targetWidth, targetHeight, true)
    } else {
        copy(config ?: Bitmap.Config.ARGB_8888, false)
    }
    return runCatching {
        val output = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, PREVIEW_JPEG_QUALITY, output)
        Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP)
    }.getOrNull().also {
        scaled.recycle()
    }
}

private fun computeCentroid(embeddings: List<FloatArray>): FloatArray? {
    val validEmbeddings = embeddings
        .map { embedding -> embedding.toFiniteArray() }
        .filter { embedding -> embedding.isNotEmpty() }
    if (validEmbeddings.isEmpty()) {
        return null
    }
    val dimension = validEmbeddings.first().size
    if (dimension <= 0) {
        return null
    }
    if (validEmbeddings.any { embedding -> embedding.size != dimension }) {
        return null
    }

    val accumulator = FloatArray(dimension)
    for (embedding in validEmbeddings) {
        for (index in 0 until dimension) {
            accumulator[index] += embedding[index]
        }
    }
    for (index in 0 until dimension) {
        accumulator[index] /= validEmbeddings.size.toFloat()
    }
    return VectorMath.l2Normalize(accumulator)
}

private fun blendEmbeddings(
    existingEmbedding: FloatArray,
    existingWeight: Int,
    currentEmbedding: FloatArray,
    currentWeight: Int
): FloatArray {
    val existing = existingEmbedding.toFiniteArray()
    val current = currentEmbedding.toFiniteArray()
    if (existing.isEmpty()) {
        return VectorMath.l2Normalize(current)
    }
    if (current.isEmpty()) {
        return VectorMath.l2Normalize(existing)
    }
    if (existing.size != current.size) {
        return VectorMath.l2Normalize(current)
    }
    val lhsWeight = existingWeight.coerceAtLeast(1).toFloat()
    val rhsWeight = currentWeight.coerceAtLeast(1).toFloat()
    val totalWeight = (lhsWeight + rhsWeight).coerceAtLeast(1f)
    val blended = FloatArray(existing.size)
    for (index in blended.indices) {
        blended[index] = (existing[index] * lhsWeight + current[index] * rhsWeight) / totalWeight
    }
    return VectorMath.l2Normalize(blended)
}

private fun cosineSimilarity(lhs: FloatArray, rhs: FloatArray): Float {
    if (lhs.isEmpty() || rhs.isEmpty() || lhs.size != rhs.size) {
        return 0f
    }
    return VectorMath.cosineSimilarity(lhs, rhs).coerceIn(-1f, 1f)
}

private fun FloatArray.toFiniteArray(): FloatArray {
    if (isEmpty()) {
        return FloatArray(0)
    }
    if (!all { value -> value.isFinite() }) {
        return FloatArray(0)
    }
    return copyOf()
}

private fun List<Float>.toFiniteArray(): FloatArray {
    if (isEmpty()) {
        return FloatArray(0)
    }
    if (!all { value -> value.isFinite() }) {
        return FloatArray(0)
    }
    return FloatArray(size) { index -> this[index] }
}

private fun List<Float>.toFiniteList(): List<Float> {
    if (isEmpty()) {
        return emptyList()
    }
    if (!all { value -> value.isFinite() }) {
        return emptyList()
    }
    return toList()
}

private fun List<Float>.averageAsFloat(): Float {
    if (isEmpty()) {
        return 0f
    }
    return (sum() / size.toFloat()).coerceIn(0f, 1f)
}

private fun weightedAverage(
    lhs: Float,
    lhsWeight: Int,
    rhs: Float,
    rhsWeight: Int
): Float {
    val left = lhsWeight.coerceAtLeast(0)
    val right = rhsWeight.coerceAtLeast(0)
    val total = left + right
    if (total <= 0) {
        return rhs.coerceIn(0f, 1f)
    }
    return ((lhs * left.toFloat()) + (rhs * right.toFloat())) / total.toFloat()
}

private fun rotateBitmapForPortrait(bitmap: Bitmap, cameraRotation: Int): Bitmap {
    if (cameraRotation != 90 && cameraRotation != 270) {
        return bitmap
    }
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

private const val FACE_MIN_DIMENSION_PX = 64
private const val FACE_READY_DIMENSION_PX = 96
private const val FACE_ASPECT_RATIO_MIN = 0.55f
private const val FACE_ASPECT_RATIO_MAX = 1.90f
private const val READY_MIN_QUALITY_SCORE = 0.30f
private const val QUALITY_SAMPLE_GRID = 24

private const val PREVIEW_CAPTURE_MIN_QUALITY_SCORE = 0.45f
private const val PREVIEW_MAX_SIZE_PX = 160
private const val PREVIEW_JPEG_QUALITY = 75


private data class ObjectEvidenceSnapshot(
    val label: String?,
    val displayConfidence: Float,
    val timestampMs: Long,
    val readyForDecision: Boolean
)

private class StableObjectEvidenceTracker(
    private val continuityGapMs: Long = 3_500L,
    private val minStableFrames: Int = 2,
    private val stableThreshold: Float = 0.74f,
    private val growthWeight: Float = 0.58f,
    private val decayFactor: Float = 0.35f
) {
    private var activeLabel: String? = null
    private var smoothedConfidence: Float = 0f
    private var stableFrames: Int = 0
    private var lastSeenAtMs: Long = 0L

    fun update(
        detection: com.aipet.brain.perception.vision.objectdetection.model.DetectedObject?,
        observedAtMs: Long
    ): ObjectEvidenceSnapshot {
        if (detection == null) {
            smoothedConfidence *= decayFactor
            if (smoothedConfidence < 0.12f) {
                activeLabel = null
                stableFrames = 0
                smoothedConfidence = 0f
            }
            lastSeenAtMs = observedAtMs
            return ObjectEvidenceSnapshot(
                label = activeLabel,
                displayConfidence = smoothedConfidence.coerceIn(0f, 1f),
                timestampMs = observedAtMs,
                readyForDecision = false
            )
        }

        val normalizedLabel = detection.label.trim().ifBlank { null }
        val confidence = detection.confidence.takeIf { it.isFinite() }?.coerceIn(0f, 1f) ?: 0f
        val sameTrack = normalizedLabel != null &&
            normalizedLabel == activeLabel &&
            observedAtMs - lastSeenAtMs <= continuityGapMs

        if (!sameTrack) {
            activeLabel = normalizedLabel
            smoothedConfidence = confidence * 0.55f
            stableFrames = 1
        } else {
            smoothedConfidence = ((smoothedConfidence * (1f - growthWeight)) + (confidence * growthWeight))
                .coerceIn(0f, 1f)
            stableFrames += 1
        }
        lastSeenAtMs = observedAtMs

        return ObjectEvidenceSnapshot(
            label = activeLabel,
            displayConfidence = smoothedConfidence,
            timestampMs = observedAtMs,
            readyForDecision = stableFrames >= minStableFrames && smoothedConfidence >= stableThreshold
        )
    }
}

private fun com.aipet.brain.perception.vision.objectdetection.model.ObjectBoundingBox.isUsableForStableTracking(): Boolean {
    val width = right - left
    val height = bottom - top
    if (width <= 0 || height <= 0) {
        return false
    }
    val aspectRatio = width.toFloat() / height.toFloat().coerceAtLeast(1f)
    return width >= 36 && height >= 36 && aspectRatio in 0.30f..3.50f
}
