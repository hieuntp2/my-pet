package com.aipet.brain.app.ui.camera

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.aipet.brain.app.settings.CameraSelection
import com.aipet.brain.perception.camera.FrameAnalyzer
import com.aipet.brain.perception.camera.FrameDiagnostics
import com.aipet.brain.perception.vision.FaceDetectionPipeline
import com.aipet.brain.perception.vision.model.FaceCropResult
import com.aipet.brain.perception.vision.model.FaceDetectionResult
import com.aipet.brain.perception.vision.objectdetection.RealObjectDetectionEngine
import com.aipet.brain.perception.vision.objectdetection.model.ObjectDetectionResult
import java.io.File
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun CameraScreen(
    hasRequestedPermission: Boolean,
    selectedCamera: CameraSelection,
    onPermissionRequestTracked: () -> Unit,
    onFrameDiagnostics: (FrameDiagnostics) -> Unit,
    onFaceDetectionResult: (FaceDetectionResult) -> Unit,
    onObjectDetected: (label: String, confidence: Float, detectedAtMs: Long) -> Unit,
    onResolveKnownObjectLabel: suspend (canonicalLabel: String) -> String?,
    onRecordPersonLikeObservation: suspend (String?) -> Result<Unit>,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val appContext = remember(context) { context.applicationContext }
    val lifecycleOwner = LocalLifecycleOwner.current
    val faceSampleStorage = remember(appContext) { FaceSampleStorage(appContext) }
    val onFrameDiagnosticsState = rememberUpdatedState(onFrameDiagnostics)
    val onFaceDetectionResultState = rememberUpdatedState(onFaceDetectionResult)
    val onObjectDetectedState = rememberUpdatedState(onObjectDetected)
    val onResolveKnownObjectLabelState = rememberUpdatedState(onResolveKnownObjectLabel)
    var permissionState by remember(context, hasRequestedPermission) {
        mutableStateOf(resolveCameraPermissionState(context, hasRequestedPermission))
    }
    var latestDiagnostics by remember { mutableStateOf<FrameDiagnostics?>(null) }
    var latestFaceDetectionResult by remember { mutableStateOf<FaceDetectionResult?>(null) }
    var latestFaceCount by remember { mutableStateOf(0) }
    var captureFaceSampleAction by remember { mutableStateOf<CaptureFaceSampleAction?>(null) }
    var captureFaceSampleMessage by remember { mutableStateOf<String?>(null) }
    var latestCapturedFaceSamplePath by remember { mutableStateOf<String?>(null) }
    var capturingFaceSample by remember { mutableStateOf(false) }
    var recordingObservation by remember { mutableStateOf(false) }
    var observationMessage by remember { mutableStateOf<String?>(null) }
    var topObjectLabelState by remember {
        mutableStateOf(CameraTopObjectLabelState.noDetection())
    }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(context, hasRequestedPermission) {
        permissionState = resolveCameraPermissionState(context, hasRequestedPermission)
    }

    LaunchedEffect(permissionState) {
        if (permissionState != CameraPermissionUiState.Granted) {
            latestDiagnostics = null
            latestFaceDetectionResult = null
            latestFaceCount = 0
            captureFaceSampleAction = null
            topObjectLabelState = CameraTopObjectLabelState.noDetection()
        }
    }
    LaunchedEffect(selectedCamera) {
        latestFaceDetectionResult = null
        latestFaceCount = 0
        captureFaceSampleAction = null
        topObjectLabelState = CameraTopObjectLabelState.noDetection()
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        permissionState = resolveCameraPermissionState(context, hasRequestedPermission = true)
    }

    val requestPermission = {
        onPermissionRequestTracked()
        permissionLauncher.launch(Manifest.permission.CAMERA)
    }
    val handleFrameDiagnostics = remember {
        { diagnostics: FrameDiagnostics ->
            latestDiagnostics = diagnostics
            onFrameDiagnosticsState.value(diagnostics)
        }
    }
    val handleFaceDetectionResult: (FaceDetectionResult) -> Unit = remember(coroutineScope) {
        { result ->
            coroutineScope.launch {
                latestFaceDetectionResult = result
                latestFaceCount = result.faces.size
                onFaceDetectionResultState.value(result)
            }
            Unit
        }
    }
    val handleObjectDetectionResult: (Result<ObjectDetectionResult>) -> Unit =
        remember(coroutineScope) {
            { result ->
                coroutineScope.launch {
                    topObjectLabelState = result.fold(
                        onSuccess = { detectionResult ->
                            val topDetection = detectionResult.detections.firstOrNull()
                            if (topDetection == null) {
                                Log.i(
                                    CAMERA_UI_TAG,
                                    "Object label UI update: no detection available; showing fallback='$NO_OBJECT_LABEL'."
                                )
                                CameraTopObjectLabelState.noDetection()
                            } else {
                                val resolvedLabel = resolveObjectLabelForDisplay(topDetection.label)
                                val canonicalLabel = resolvedLabel.label
                                val normalizedConfidence = topDetection.confidence
                                    .takeIf { it.isFinite() }
                                    ?.coerceIn(0f, 1f)
                                val preferredDisplayLabel = if (resolvedLabel.fallbackReason == null) {
                                    runCatching {
                                        onResolveKnownObjectLabelState.value(canonicalLabel)
                                    }.onFailure { lookupError ->
                                        Log.w(
                                            CAMERA_UI_TAG,
                                            "Known-object label lookup failed for canonicalLabel='$canonicalLabel'.",
                                            lookupError
                                        )
                                    }.getOrNull()
                                        ?.trim()
                                        ?.takeIf { candidate -> candidate.isNotBlank() }
                                        ?: canonicalLabel
                                } else {
                                    canonicalLabel
                                }
                                if (resolvedLabel.fallbackReason == null) {
                                    if (preferredDisplayLabel == canonicalLabel) {
                                        Log.i(
                                            CAMERA_UI_TAG,
                                            "Object label UI update: canonicalLabel='$canonicalLabel', " +
                                                "displayLabel='$preferredDisplayLabel', confidence=${formatConfidence(normalizedConfidence)}."
                                        )
                                    } else {
                                        Log.i(
                                            CAMERA_UI_TAG,
                                            "Object label UI alias applied: canonicalLabel='$canonicalLabel', " +
                                                "displayLabel='$preferredDisplayLabel', confidence=${formatConfidence(normalizedConfidence)}."
                                        )
                                    }
                                    if (normalizedConfidence == null) {
                                        Log.w(
                                            CAMERA_UI_TAG,
                                            "Skipping object event callback: label='$canonicalLabel', " +
                                                "reason='invalid_confidence'."
                                        )
                                    } else {
                                        runCatching {
                                            onObjectDetectedState.value(
                                                canonicalLabel,
                                                normalizedConfidence,
                                                detectionResult.timestampMs
                                            )
                                        }.onSuccess {
                                            Log.i(
                                                CAMERA_UI_TAG,
                                                "Object event candidate dispatched: label='$canonicalLabel', " +
                                                    "confidence=${formatConfidence(normalizedConfidence)}, " +
                                                    "detectedAtMs=${detectionResult.timestampMs}."
                                            )
                                        }.onFailure { callbackError ->
                                            Log.e(
                                                CAMERA_UI_TAG,
                                                "Object event callback failed for label='$canonicalLabel'.",
                                                callbackError
                                            )
                                        }
                                    }
                                } else {
                                    Log.i(
                                        CAMERA_UI_TAG,
                                        "Object label UI update: rawLabel='${topDetection.label}', " +
                                            "confidence=${formatConfidence(normalizedConfidence)}, " +
                                            "fallback='${resolvedLabel.fallbackReason}', " +
                                            "displayLabel='$preferredDisplayLabel'."
                                    )
                                    Log.i(
                                        CAMERA_UI_TAG,
                                        "Skipping object event callback due to fallback label mapping."
                                    )
                                }
                                CameraTopObjectLabelState.detected(
                                    displayLabel = preferredDisplayLabel,
                                    confidence = normalizedConfidence
                                )
                            }
                        },
                        onFailure = { error ->
                            Log.w(
                                CAMERA_UI_TAG,
                                "Object label UI update failed; showing fallback='$NO_OBJECT_LABEL'.",
                                error
                            )
                            CameraTopObjectLabelState.noDetection()
                        }
                    )
                }
                Unit
            }
        }

    DisposableEffect(lifecycleOwner, context, hasRequestedPermission) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionState = resolveCameraPermissionState(context, hasRequestedPermission)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(text = "Camera")
        Text(
            text = "Phase 1 Camera preview (C17)",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
        )

        when (val state = permissionState) {
            CameraPermissionUiState.NotRequested -> {
                Text(text = "Camera permission has not been granted yet.")
                Button(
                    onClick = requestPermission,
                    modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                ) {
                    Text(text = "Request Camera Permission")
                }
            }

            is CameraPermissionUiState.Denied -> {
                if (state.canRequestAgain) {
                    Text(text = "Camera permission was denied.")
                    Text(
                        text = "Please allow camera access to continue.",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = requestPermission,
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Text(text = "Request Again")
                    }
                } else {
                    Text(text = "Camera permission is permanently denied.")
                    Text(
                        text = "Enable camera permission in app settings, then return.",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = { openAppSettings(context) },
                        modifier = Modifier.padding(top = 12.dp, bottom = 8.dp)
                    ) {
                        Text(text = "Open App Settings")
                    }
                }
            }

            CameraPermissionUiState.Granted -> {
                Text(text = "Camera permission granted.")
                Text(
                    text = "Live preview is running.",
                    modifier = Modifier.padding(top = 4.dp)
                )
                Text(
                    text = "Requested camera: ${selectedCamera.displayName}",
                    modifier = Modifier.padding(top = 4.dp)
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(vertical = 8.dp)
                ) {
                    CameraPreview(
                        modifier = Modifier.fillMaxSize(),
                        selectedCamera = selectedCamera,
                        onFrameDiagnostics = handleFrameDiagnostics,
                        onFaceDetectionResult = handleFaceDetectionResult,
                        onObjectDetectionResult = handleObjectDetectionResult,
                        onCaptureFaceSampleActionChanged = { action ->
                            captureFaceSampleAction = action
                        }
                    )
                    FaceOverlay(
                        faceDetectionResult = latestFaceDetectionResult,
                        isFrontCamera = selectedCamera == CameraSelection.FRONT,
                        modifier = Modifier.matchParentSize()
                    )
                    CameraDiagnosticsOverlay(
                        diagnostics = latestDiagnostics,
                        faceCount = latestFaceCount,
                        topObjectLabel = topObjectLabelState.displayLabel,
                        topObjectConfidence = topObjectLabelState.confidence,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(12.dp)
                    )
                }

                val canCaptureFaceSample = latestFaceCount > 0 &&
                    captureFaceSampleAction != null &&
                    !capturingFaceSample
                Button(
                    onClick = {
                        val captureAction = captureFaceSampleAction
                        if (capturingFaceSample) {
                            return@Button
                        }
                        if (captureAction == null) {
                            captureFaceSampleMessage =
                                "Capture failed: no recent frame is available for cropping."
                            return@Button
                        }

                        capturingFaceSample = true
                        captureFaceSampleMessage = "Capturing face sample..."
                        coroutineScope.launch {
                            val cropResult = withContext(Dispatchers.Default) {
                                captureAction()
                            }

                            val captureMessage = if (cropResult.isSuccess) {
                                val croppedBitmap = cropResult.bitmap
                                if (croppedBitmap == null) {
                                    "Capture failed: cropped face image was empty."
                                } else {
                                    val saveResult = runCatching {
                                        faceSampleStorage.save(croppedBitmap)
                                    }
                                    croppedBitmap.recycle()
                                    saveResult.fold(
                                        onSuccess = { outputFile ->
                                            latestCapturedFaceSamplePath = outputFile.absolutePath
                                            "Face sample saved: ${outputFile.name} " +
                                                "(${cropResult.cropWidth}x${cropResult.cropHeight})"
                                        },
                                        onFailure = { error ->
                                            "Capture failed: ${error.message ?: "unable to write image file"}"
                                        }
                                    )
                                }
                            } else {
                                "Capture failed: ${cropResult.failureReason?.name ?: "UNKNOWN"}"
                            }

                            captureFaceSampleMessage = captureMessage
                            capturingFaceSample = false
                        }
                    },
                    enabled = canCaptureFaceSample,
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = if (capturingFaceSample) "Capturing..." else "Capture Face Sample")
                }

                if (captureFaceSampleMessage != null) {
                    Text(
                        text = captureFaceSampleMessage.orEmpty(),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                if (latestCapturedFaceSamplePath != null) {
                    Text(
                        text = "Latest face sample: ${latestCapturedFaceSamplePath.orEmpty()}",
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Button(
                    onClick = {
                        if (recordingObservation) {
                            return@Button
                        }
                        recordingObservation = true
                        observationMessage = "Recording person-like observation..."
                        val note = latestDiagnostics?.let { diagnostics ->
                            "frame=${diagnostics.width}x${diagnostics.height}, " +
                                "processingMs=${diagnostics.processingDurationMs}"
                        } ?: "camera_preview_active"
                        coroutineScope.launch {
                            val recordResult = onRecordPersonLikeObservation(note)
                            observationMessage = recordResult.fold(
                                onSuccess = { "Person-like observation recorded." },
                                onFailure = { error ->
                                    "Observation record failed: ${error.message ?: "Unknown error"}"
                                }
                            )
                            recordingObservation = false
                        }
                    },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(text = if (recordingObservation) "Recording..." else "Record Person-like Observation")
                }

                if (observationMessage != null) {
                    Text(
                        text = observationMessage.orEmpty(),
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        Button(onClick = onNavigateBack, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)) {
            Text(text = "Back to Home")
        }
    }
}

@Composable
private fun CameraPreview(
    modifier: Modifier = Modifier,
    selectedCamera: CameraSelection,
    onFrameDiagnostics: (FrameDiagnostics) -> Unit,
    onFaceDetectionResult: (FaceDetectionResult) -> Unit,
    onObjectDetectionResult: (Result<ObjectDetectionResult>) -> Unit,
    onCaptureFaceSampleActionChanged: (CaptureFaceSampleAction?) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember(context) {
        PreviewView(context).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }
    val onFrameDiagnosticsState = rememberUpdatedState(onFrameDiagnostics)
    val onFaceDetectionResultState = rememberUpdatedState(onFaceDetectionResult)
    val onObjectDetectionResultState = rememberUpdatedState(onObjectDetectionResult)
    val faceDetectionPipeline = remember {
        FaceDetectionPipeline { result ->
            onFaceDetectionResultState.value(result)
        }
    }
    val objectDetectionEngine = remember(context) {
        runCatching {
            RealObjectDetectionEngine(context.assets)
        }.onFailure { error ->
            Log.e(
                CAMERA_UI_TAG,
                "Failed to initialize object detection engine.",
                error
            )
        }.getOrNull()
    }
    val frameAnalyzer = remember {
        FrameAnalyzer(
            faceDetectionPipeline = faceDetectionPipeline,
            onDiagnostics = { diagnostics ->
                onFrameDiagnosticsState.value(diagnostics)
            },
            objectDetectionEngine = objectDetectionEngine,
            onObjectDetectionResult = { detectionResult ->
                onObjectDetectionResultState.value(detectionResult)
            }
        )
    }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    var cameraProvider by remember { mutableStateOf<ProcessCameraProvider?>(null) }
    var imageAnalysis by remember { mutableStateOf<ImageAnalysis?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    DisposableEffect(analysisExecutor) {
        onDispose {
            analysisExecutor.shutdownSafely()
        }
    }
    DisposableEffect(faceDetectionPipeline, onCaptureFaceSampleActionChanged) {
        onCaptureFaceSampleActionChanged {
            faceDetectionPipeline.captureLargestFaceSample()
        }
        onDispose {
            onCaptureFaceSampleActionChanged(null)
            faceDetectionPipeline.close()
        }
    }
    DisposableEffect(objectDetectionEngine) {
        onDispose {
            objectDetectionEngine?.close()
        }
    }

    DisposableEffect(
        lifecycleOwner,
        previewView,
        context,
        analysisExecutor,
        frameAnalyzer,
        selectedCamera
    ) {
        var disposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
        cameraProviderFuture.addListener(
            {
                if (disposed) {
                    return@addListener
                }
                try {
                    val provider = cameraProviderFuture.get()
                    val previewUseCase = Preview.Builder().build().also { preview ->
                        preview.setSurfaceProvider(previewView.surfaceProvider)
                    }
                    val analysisUseCase = ImageAnalysis.Builder()
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { useCase ->
                            useCase.setAnalyzer(analysisExecutor, frameAnalyzer)
                        }
                    val availability = resolveCameraAvailability(provider)
                    val cameraToUse = CameraSelectionResolver.resolve(
                        selectedCamera = selectedCamera,
                        availability = availability
                    )
                    if (cameraToUse == null) {
                        errorMessage = "No usable camera is available on this device."
                        cameraProvider = provider
                        imageAnalysis = analysisUseCase
                        return@addListener
                    }

                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        cameraToUse.toCameraSelector(),
                        previewUseCase,
                        analysisUseCase
                    )
                    cameraProvider = provider
                    imageAnalysis = analysisUseCase
                    errorMessage = null
                } catch (_: Exception) {
                    errorMessage = "Unable to start camera preview on this device."
                }
            },
            ContextCompat.getMainExecutor(context)
        )

        onDispose {
            disposed = true
            imageAnalysis?.clearAnalyzer()
            cameraProvider?.unbindAll()
            imageAnalysis = null
        }
    }

    if (errorMessage == null) {
        AndroidView(
            factory = { previewView },
            modifier = modifier
        )
    } else {
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text(text = errorMessage.orEmpty())
        }
    }
}

private fun resolveCameraPermissionState(
    context: Context,
    hasRequestedPermission: Boolean
): CameraPermissionUiState {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.CAMERA
    ) == PackageManager.PERMISSION_GRANTED
    if (granted) {
        return CameraPermissionUiState.Granted
    }
    if (!hasRequestedPermission) {
        return CameraPermissionUiState.NotRequested
    }

    val activity = context.findActivity()
    val canRequestAgain = activity?.let {
        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.CAMERA)
    } ?: false

    return CameraPermissionUiState.Denied(canRequestAgain = canRequestAgain)
}

private fun openAppSettings(context: Context) {
    val detailsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    val fallbackIntent = Intent(Settings.ACTION_SETTINGS).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }

    try {
        context.startActivity(detailsIntent)
    } catch (_: ActivityNotFoundException) {
        try {
            context.startActivity(fallbackIntent)
        } catch (_: ActivityNotFoundException) {
            // No available settings handler on this device.
        }
    }
}

private tailrec fun Context.findActivity(): Activity? {
    return when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
}

private fun ExecutorService.shutdownSafely() {
    if (!isShutdown) {
        shutdown()
    }
}

private fun resolveCameraAvailability(provider: ProcessCameraProvider): CameraAvailability {
    val hasFront = runCatching {
        provider.hasCamera(CameraSelector.DEFAULT_FRONT_CAMERA)
    }.getOrDefault(false)
    val hasBack = runCatching {
        provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)
    }.getOrDefault(false)
    return CameraAvailability(
        hasFrontCamera = hasFront,
        hasBackCamera = hasBack
    )
}

private fun CameraSelection.toCameraSelector(): CameraSelector {
    return when (this) {
        CameraSelection.FRONT -> CameraSelector.DEFAULT_FRONT_CAMERA
        CameraSelection.BACK -> CameraSelector.DEFAULT_BACK_CAMERA
    }
}

private sealed interface CameraPermissionUiState {
    object NotRequested : CameraPermissionUiState
    data class Denied(val canRequestAgain: Boolean) : CameraPermissionUiState
    object Granted : CameraPermissionUiState
}

private class FaceSampleStorage(
    private val appContext: Context
) {
    fun save(faceBitmap: Bitmap): File {
        val outputDirectory = File(appContext.filesDir, FACE_SAMPLE_DIRECTORY).also { directory ->
            if (!directory.exists()) {
                check(directory.mkdirs()) {
                    "Unable to create face sample directory."
                }
            }
        }

        val fileName = buildString {
            append("face_sample_")
            append(System.currentTimeMillis())
            append("_")
            append(UUID.randomUUID().toString().replace("-", "").take(8))
            append(".jpg")
        }
        val outputFile = File(outputDirectory, fileName)
        outputFile.outputStream().use { output ->
            check(faceBitmap.compress(Bitmap.CompressFormat.JPEG, FACE_SAMPLE_JPEG_QUALITY, output)) {
                "Unable to encode face sample image."
            }
        }
        return outputFile
    }
}

private typealias CaptureFaceSampleAction = () -> FaceCropResult

private data class CameraTopObjectLabelState(
    val displayLabel: String,
    val confidence: Float?
) {
    companion object {
        fun noDetection(): CameraTopObjectLabelState {
            return CameraTopObjectLabelState(
                displayLabel = NO_OBJECT_LABEL,
                confidence = null
            )
        }

        fun detected(
            displayLabel: String,
            confidence: Float?
        ): CameraTopObjectLabelState {
            return CameraTopObjectLabelState(
                displayLabel = displayLabel,
                confidence = confidence
            )
        }
    }
}

private data class ResolvedObjectLabel(
    val label: String,
    val fallbackReason: String? = null
)

private fun resolveObjectLabelForDisplay(rawLabel: String?): ResolvedObjectLabel {
    val sanitized = rawLabel?.trim().orEmpty()
    if (sanitized.isBlank()) {
        return ResolvedObjectLabel(
            label = UNKNOWN_OBJECT_LABEL,
            fallbackReason = "missing_label"
        )
    }
    if (sanitized == OBJECT_UNKNOWN_TOKEN || sanitized.startsWith(OBJECT_CLASS_FALLBACK_PREFIX)) {
        return ResolvedObjectLabel(
            label = UNKNOWN_OBJECT_LABEL,
            fallbackReason = "class_id_fallback:$sanitized"
        )
    }
    return ResolvedObjectLabel(label = sanitized)
}

private fun formatConfidence(confidence: Float?): String {
    return confidence?.let { value ->
        String.format(Locale.US, "%.3f", value)
    } ?: "n/a"
}

private const val FACE_SAMPLE_DIRECTORY = "face_samples/captured"
private const val FACE_SAMPLE_JPEG_QUALITY = 92
private const val CAMERA_UI_TAG = "CameraObjectLabel"
private const val NO_OBJECT_LABEL = "none"
private const val UNKNOWN_OBJECT_LABEL = "Unknown object"
private const val OBJECT_UNKNOWN_TOKEN = "???"
private const val OBJECT_CLASS_FALLBACK_PREFIX = "class_"
