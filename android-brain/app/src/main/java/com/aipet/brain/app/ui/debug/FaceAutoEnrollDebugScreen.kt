package com.aipet.brain.app.ui.debug

import android.Manifest
import android.graphics.Bitmap
import android.graphics.Matrix
import android.util.Log
import android.util.Size
import android.hardware.camera2.CaptureRequest
import android.util.Range
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.camera2.interop.Camera2Interop
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.aipet.brain.brain.recognition.KnownPersonEmbeddingsSource
import com.aipet.brain.brain.recognition.PersonRecognitionService
import com.aipet.brain.brain.recognition.model.KnownPersonEmbeddings
import com.aipet.brain.memory.profiles.FaceProfileStore
import com.aipet.brain.perception.camera.FrameAnalyzer
import com.aipet.brain.perception.vision.FaceDetectionPipeline
import com.aipet.brain.perception.vision.face.embedding.FaceEmbeddingEngine
import java.util.Locale
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Minimum recognition score to auto-capture the frame as a new embedding.
private const val AUTO_ENROLL_CAPTURE_THRESHOLD = 0.65f

// Minimum pairwise cosine similarity mean across all embeddings to declare profile validated.
private const val VALIDATION_PASS_THRESHOLD = 0.60f

// Target number of embeddings before auto-stop.
private const val ENROLL_TARGET_SAMPLES = 10

// Hard cap on total embeddings per person (including pre-existing ones).
private const val ENROLL_MAX_TOTAL_EMBEDDINGS = 30

// How often (ms) the validation recomputation runs.
private const val VALIDATION_INTERVAL_MS = 3_000L

private const val TAG = "FaceAutoEnrollDebug"

// ── State types ────────────────────────────────────────────────────────────────

private enum class EnrollPhase { IDLE, RUNNING, PAUSED, DONE }

private data class EnrollState(
    val phase: EnrollPhase = EnrollPhase.IDLE,
    val capturedThisSession: Int = 0,
    val totalEmbeddings: Int = 0,
    val lastScore: Float = 0f,
    val validationMean: Float = 0f,
    val validationSampleCount: Int = 0,
    val statusMessage: String = "Select a person and press Start.",
    val errorMessage: String? = null
)

// ── Main composable ────────────────────────────────────────────────────────────

@Composable
internal fun FaceAutoEnrollDebugScreen(
    knownPersonEmbeddingsSource: KnownPersonEmbeddingsSource,
    personRecognitionService: PersonRecognitionService,
    faceEmbeddingEngine: FaceEmbeddingEngine,
    faceProfileStore: FaceProfileStore,
    onNavigateBack: () -> Unit,
    hasRequestedCameraPermission: Boolean,
    onPermissionRequestTracked: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    var cameraPermissionGranted by remember(hasRequestedCameraPermission) {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        cameraPermissionGranted = granted
    }

    // Load persons from DB
    var knownPersons by remember { mutableStateOf<List<KnownPersonEmbeddings>>(emptyList()) }
    LaunchedEffect(Unit) {
        knownPersons = withContext(Dispatchers.IO) {
            knownPersonEmbeddingsSource.loadKnownPersonEmbeddings()
        }
    }

    var selectedPersonIndex by remember { mutableIntStateOf(0) }
    val selectedPerson = knownPersons.getOrNull(selectedPersonIndex)

    var enrollState by remember { mutableStateOf(EnrollState()) }

    // Holds the most recent face crop bitmap ready for processing.
    val pendingBitmapRef = remember {
        java.util.concurrent.atomic.AtomicReference<Bitmap?>(null)
    }

    // Core enroll loop — consumes face crop bitmaps while RUNNING.
    LaunchedEffect(enrollState.phase, selectedPerson?.personId) {
        if (enrollState.phase != EnrollPhase.RUNNING) return@LaunchedEffect
        val targetPerson = selectedPerson ?: return@LaunchedEffect

        while (true) {
            // Re-check phase each iteration so pause/stop are respected.
            if (enrollState.phase != EnrollPhase.RUNNING) break

            val bitmap = pendingBitmapRef.getAndSet(null)
            if (bitmap != null) {
                try {
                    val embeddingResult = withContext(Dispatchers.Default) {
                        faceEmbeddingEngine.generateEmbedding(bitmap)
                    }
                    embeddingResult.onSuccess { embedding ->
                        val recognitionResult = withContext(Dispatchers.Default) {
                            personRecognitionService.recognize(embedding)
                        }
                        val score = recognitionResult.bestScore
                        val isSamePerson = recognitionResult.accepted &&
                            recognitionResult.bestPersonId == targetPerson.personId &&
                            score >= AUTO_ENROLL_CAPTURE_THRESHOLD

                        if (isSamePerson) {
                            val profiles = withContext(Dispatchers.IO) {
                                faceProfileStore.listProfilesForPerson(targetPerson.personId)
                            }
                            val primaryProfile = profiles.firstOrNull()
                            if (primaryProfile != null) {
                                val existingEmbeddings = withContext(Dispatchers.IO) {
                                    faceProfileStore.listProfileEmbeddings(primaryProfile.profileId)
                                }
                                if (existingEmbeddings.size < ENROLL_MAX_TOTAL_EMBEDDINGS) {
                                    withContext(Dispatchers.IO) {
                                        faceProfileStore.addEmbeddingToProfile(
                                            profileId = primaryProfile.profileId,
                                            values = embedding.toList(),
                                            metadata = "auto_enroll_debug"
                                        )
                                    }
                                    val newTotal = existingEmbeddings.size + 1
                                    val newCaptured = enrollState.capturedThisSession + 1
                                    enrollState = enrollState.copy(
                                        capturedThisSession = newCaptured,
                                        totalEmbeddings = newTotal,
                                        lastScore = score,
                                        statusMessage = "Captured sample $newCaptured " +
                                            "(score ${String.format(Locale.US, "%.2f", score)})."
                                    )
                                    Log.d(
                                        TAG,
                                        "Auto-enrolled sample for ${targetPerson.displayName}, " +
                                            "score=$score, total=$newTotal"
                                    )
                                } else {
                                    enrollState = enrollState.copy(
                                        phase = EnrollPhase.DONE,
                                        statusMessage = "Max embeddings ($ENROLL_MAX_TOTAL_EMBEDDINGS) reached for this person."
                                    )
                                }
                            } else {
                                enrollState = enrollState.copy(
                                    lastScore = score,
                                    statusMessage = "No face profile linked to ${targetPerson.displayName}. Teach first."
                                )
                            }
                        } else {
                            enrollState = enrollState.copy(
                                lastScore = score,
                                statusMessage = when {
                                    score in 0.35f..AUTO_ENROLL_CAPTURE_THRESHOLD ->
                                        "Near-miss score ${String.format(Locale.US, "%.2f", score)} — move closer."
                                    else ->
                                        "Waiting for confident match… score=${String.format(Locale.US, "%.2f", score)}"
                                }
                            )
                        }
                    }.onFailure { error ->
                        Log.w(TAG, "Embedding failed: ${error.message}")
                    }
                } finally {
                    bitmap.recycle()
                }
            }
            delay(200L)
        }
    }

    // Validation loop — computes pairwise cosine similarity periodically.
    LaunchedEffect(enrollState.phase, selectedPerson?.personId) {
        if (enrollState.phase == EnrollPhase.IDLE || enrollState.phase == EnrollPhase.DONE) return@LaunchedEffect
        val targetPerson = selectedPerson ?: return@LaunchedEffect

        while (enrollState.phase == EnrollPhase.RUNNING || enrollState.phase == EnrollPhase.PAUSED) {
            delay(VALIDATION_INTERVAL_MS)
            if (enrollState.capturedThisSession == 0) continue

            val (mean, n) = withContext(Dispatchers.IO) {
                computeValidationScore(
                    personId = targetPerson.personId,
                    faceProfileStore = faceProfileStore
                )
            }
            enrollState = enrollState.copy(
                validationMean = mean,
                validationSampleCount = n
            )
            val passed = mean >= VALIDATION_PASS_THRESHOLD && n >= ENROLL_TARGET_SAMPLES
            if (passed && enrollState.phase == EnrollPhase.RUNNING) {
                enrollState = enrollState.copy(
                    phase = EnrollPhase.DONE,
                    statusMessage = "Validation passed! $n embeddings, " +
                        "mean similarity ${String.format(Locale.US, "%.2f", mean)}. Profile ready."
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {

        // ── Camera preview (top portion) ──────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (cameraPermissionGranted) {
                AutoEnrollCameraPreview(
                    modifier = Modifier.fillMaxSize(),
                    onLiveFaceCropReady = { bmp, _, cameraRotation ->
                        // Rotate portrait crops to landscape orientation to match
                        // stored embeddings that were generated from raw sensor frames.
                        val oriented = rotateBitmapForPortrait(bmp, cameraRotation)
                        pendingBitmapRef.getAndSet(oriented)?.recycle()
                    },
                    lifecycleOwner = lifecycleOwner
                )

                // Score overlay (top-left)
                val scoreColor = when {
                    enrollState.lastScore >= AUTO_ENROLL_CAPTURE_THRESHOLD -> Color(0xFF90EE90)
                    enrollState.lastScore >= 0.35f -> Color(0xFFFFB74D)
                    else -> Color.White
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Target: ${selectedPerson?.displayName ?: "—"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        Text(
                            text = "Score: ${String.format(Locale.US, "%.2f", enrollState.lastScore)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = scoreColor
                        )
                        Text(
                            text = "Captured: ${enrollState.capturedThisSession} | Total: ${enrollState.totalEmbeddings}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White
                        )
                        val validationColor = if (enrollState.validationMean >= VALIDATION_PASS_THRESHOLD) {
                            Color(0xFF90EE90)
                        } else {
                            Color.White
                        }
                        Text(
                            text = "Validation: ${String.format(Locale.US, "%.2f", enrollState.validationMean)} (${enrollState.validationSampleCount})",
                            style = MaterialTheme.typography.bodySmall,
                            color = validationColor
                        )
                        Text(
                            text = "Capture≥$AUTO_ENROLL_CAPTURE_THRESHOLD | Valid≥$VALIDATION_PASS_THRESHOLD",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.75f)
                        )
                    }
                }

                // Phase badge (top-right)
                val (phaseText, phaseColor) = when (enrollState.phase) {
                    EnrollPhase.RUNNING -> "RUNNING" to Color(0xFF90EE90)
                    EnrollPhase.PAUSED -> "PAUSED" to Color(0xFFFFB74D)
                    EnrollPhase.DONE -> "DONE" to Color(0xFF64B5F6)
                    EnrollPhase.IDLE -> "IDLE" to Color.White
                }
                Text(
                    text = phaseText,
                    style = MaterialTheme.typography.labelLarge,
                    color = phaseColor,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(10.dp)
                        .background(Color.Black.copy(alpha = 0.65f), MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                )
            } else {
                // Permission not granted
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = "Camera permission required.")
                        Button(onClick = {
                            onPermissionRequestTracked()
                            permissionLauncher.launch(Manifest.permission.CAMERA)
                        }) {
                            Text(text = "Grant Permission")
                        }
                    }
                }
            }
        }

        // ── Control panel (bottom) ────────────────────────────────────────────
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "Face Auto-Enroll Debug",
                style = MaterialTheme.typography.headlineSmall
            )

            // Target person selector card
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Target Person",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (knownPersons.isEmpty()) {
                        Text(
                            text = "No persons found — teach someone first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = {
                                    selectedPersonIndex =
                                        (selectedPersonIndex - 1 + knownPersons.size) % knownPersons.size
                                    enrollState = EnrollState()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(text = "◀") }
                            Text(
                                text = selectedPerson?.displayName ?: "—",
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedButton(
                                onClick = {
                                    selectedPersonIndex =
                                        (selectedPersonIndex + 1) % knownPersons.size
                                    enrollState = EnrollState()
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text(text = "▶") }
                        }
                        selectedPerson?.let { person ->
                            Text(
                                text = "Known embeddings: ${person.embeddings.size}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // How it works
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "1. Camera scans your face continuously (~1 fps).",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2. When recognition score ≥ $AUTO_ENROLL_CAPTURE_THRESHOLD, the frame is saved as a new embedding.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "2b. Portrait frames are auto-rotated to landscape orientation before embedding.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "3. Every ${VALIDATION_INTERVAL_MS / 1000}s, pairwise cosine similarity is computed across all embeddings.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "4. Auto-stops when mean ≥ $VALIDATION_PASS_THRESHOLD and $ENROLL_TARGET_SAMPLES+ samples exist.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // Status messages
            if (enrollState.statusMessage.isNotBlank()) {
                Text(
                    text = enrollState.statusMessage,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            if (enrollState.errorMessage != null) {
                Text(
                    text = enrollState.errorMessage!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val canStart = selectedPerson != null &&
                    enrollState.phase != EnrollPhase.RUNNING
                Button(
                    onClick = {
                        enrollState = enrollState.copy(
                            phase = EnrollPhase.RUNNING,
                            capturedThisSession = 0,
                            lastScore = 0f,
                            statusMessage = "Running… waiting for confident face match.",
                            errorMessage = null
                        )
                        // Refresh person list and total count in background.
                        coroutineScope.launch {
                            knownPersons = withContext(Dispatchers.IO) {
                                knownPersonEmbeddingsSource.loadKnownPersonEmbeddings()
                            }
                            val refreshedPerson = knownPersons.getOrNull(selectedPersonIndex)
                            if (refreshedPerson != null) {
                                val profiles = withContext(Dispatchers.IO) {
                                    faceProfileStore.listProfilesForPerson(refreshedPerson.personId)
                                }
                                val total = profiles.sumOf { profile ->
                                    withContext(Dispatchers.IO) {
                                        faceProfileStore.listProfileEmbeddings(profile.profileId).size
                                    }
                                }
                                enrollState = enrollState.copy(totalEmbeddings = total)
                            }
                        }
                    },
                    enabled = canStart && cameraPermissionGranted,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = when (enrollState.phase) {
                            EnrollPhase.DONE -> "Restart"
                            else -> "Start"
                        }
                    )
                }
                OutlinedButton(
                    onClick = {
                        enrollState = enrollState.copy(
                            phase = if (enrollState.phase == EnrollPhase.RUNNING) {
                                EnrollPhase.PAUSED
                            } else {
                                EnrollPhase.RUNNING
                            },
                            statusMessage = if (enrollState.phase == EnrollPhase.RUNNING) {
                                "Paused."
                            } else {
                                "Resumed."
                            }
                        )
                    },
                    enabled = enrollState.phase == EnrollPhase.RUNNING ||
                        enrollState.phase == EnrollPhase.PAUSED,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = if (enrollState.phase == EnrollPhase.RUNNING) "Pause" else "Resume"
                    )
                }
            }

            OutlinedButton(
                onClick = {
                    enrollState = EnrollState(
                        statusMessage = "Reset. Select a person and press Start."
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Reset Session")
            }

            OutlinedButton(
                onClick = onNavigateBack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(text = "Back to Debug")
            }
        }
    }
}

// ── Pairwise validation ────────────────────────────────────────────────────────

private suspend fun computeValidationScore(
    personId: String,
    faceProfileStore: FaceProfileStore
): Pair<Float, Int> {
    val profiles = faceProfileStore.listProfilesForPerson(personId)
    if (profiles.isEmpty()) return 0f to 0
    val allEmbeddings = profiles.flatMap { profile ->
        faceProfileStore.listProfileEmbeddings(profile.profileId)
    }.map { record ->
        record.values.toFloatArray()
    }.filter { it.isNotEmpty() }
    if (allEmbeddings.size < 2) return 0f to allEmbeddings.size

    var sum = 0.0
    var pairs = 0
    for (i in allEmbeddings.indices) {
        for (j in i + 1 until allEmbeddings.size) {
            sum += pairwiseCosineSimilarity(allEmbeddings[i], allEmbeddings[j])
            pairs++
        }
    }
    return if (pairs == 0) 0f to allEmbeddings.size else (sum / pairs).toFloat() to allEmbeddings.size
}

private fun pairwiseCosineSimilarity(a: FloatArray, b: FloatArray): Double {
    if (a.size != b.size || a.isEmpty()) return 0.0
    var dot = 0.0
    var normA = 0.0
    var normB = 0.0
    for (i in a.indices) {
        dot += a[i] * b[i]
        normA += a[i] * a[i]
        normB += b[i] * b[i]
    }
    val denom = Math.sqrt(normA) * Math.sqrt(normB)
    return if (denom == 0.0) 0.0 else dot / denom
}

/**
 * Rotate 90° CW when camera reports portrait orientation (90° or 270°) so that face
 * crops are in landscape-equivalent orientation, matching embeddings stored from
 * raw-sensor (landscape) teaching captures. The original bitmap is recycled.
 */
private fun rotateBitmapForPortrait(bitmap: Bitmap, cameraRotation: Int): Bitmap {
    if (cameraRotation != 90 && cameraRotation != 270) return bitmap
    val matrix = Matrix().apply { postRotate(90f) }
    val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    bitmap.recycle()
    return rotated
}

// ── Dedicated camera preview for auto-enroll ──────────────────────────────────

@Composable
private fun AutoEnrollCameraPreview(
    modifier: Modifier = Modifier,
    onLiveFaceCropReady: (Bitmap, Long, Int) -> Unit,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner
) {
    val context = LocalContext.current
    val onCropReadyState = rememberUpdatedState(onLiveFaceCropReady)

    val previewView = remember {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    val faceDetectionPipeline = remember {
        FaceDetectionPipeline(
            onFacesDetected = { /* overlays not needed for enroll preview */ },
            onLiveFaceCropReady = { bmp, ts, rotation -> onCropReadyState.value(bmp, ts, rotation) },
            liveFaceCropIntervalMs = 1_000L
        )
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().apply {
                surfaceProvider = previewView.surfaceProvider
            }
            val analysisBuilder = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .setResolutionSelector(
                    ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                Size(640, 480),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                )
            Camera2Interop.Extender(analysisBuilder)
                .setCaptureRequestOption(
                    CaptureRequest.CONTROL_AE_TARGET_FPS_RANGE,
                    Range(10, 15)
                )
            val frameAnalyzer = FrameAnalyzer(
                faceDetectionPipeline = faceDetectionPipeline
            )
            val analysis = analysisBuilder.build().also { ia ->
                ia.setAnalyzer(executor, frameAnalyzer)
            }
            runCatching {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(
                    lifecycleOwner,
                    CameraSelector.DEFAULT_FRONT_CAMERA,
                    preview,
                    analysis
                )
            }.onFailure { e ->
                Log.e(TAG, "Failed to bind auto-enroll camera: ${e.message}", e)
            }
        }, ContextCompat.getMainExecutor(context))

        onDispose {
            faceDetectionPipeline.close()
            executor.shutdown()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}
